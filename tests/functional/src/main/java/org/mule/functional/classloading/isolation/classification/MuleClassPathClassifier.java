/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Class.forName;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFromHierarchy;
import org.mule.functional.classloading.isolation.classpath.DefaultMavenArtifactToClassPathURLResolver;
import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.classloading.isolation.maven.MavenArtifactMatcherPredicate;
import org.mule.functional.classloading.isolation.maven.MavenArtifactToClassPathURLResolver;
import org.mule.functional.classloading.isolation.maven.MavenMultiModuleArtifactMapping;
import org.mule.functional.classloading.isolation.maven.dependencies.ConfigurationBuilder;
import org.mule.functional.classloading.isolation.maven.dependencies.DependenciesFilterBuilder;
import org.mule.functional.classloading.isolation.maven.dependencies.DependencyResolver;
import org.mule.functional.classloading.isolation.maven.dependencies.TransitiveDependenciesFilterBuilder;
import org.mule.functional.classloading.isolation.utils.RunnerModuleUtils;
import org.mule.functional.junit4.ExtensionsTestInfrastructureDiscoverer;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.registry.DefaultRegistryBroker;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;

import com.google.common.collect.Lists;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Default implementation for {@link ClassPathClassifier} that builds a {@link ArtifactUrlClassification} similar to what Mule
 * Runtime does by taking into account the Maven dependencies of the given tested artifact.
 * <p/>
 * Basically it creates a {@link ArtifactUrlClassification} hierarchy with:
 * Provided Scope (plus JDK stuff)->Composite ClassLoader(that includes a class loader for each extension and its compile scope dependencies (plus its target/classes))->Test Scope (plus target/test-classes and all the test scope dependencies including transitives)
 *
 * @since 4.0
 */
public class MuleClassPathClassifier implements ClassPathClassifier
{

    public static final String GENERATED_TEST_SOURCES = "generated-test-sources";
    private static final String TARGET_FOLDER_NAME = "target";

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ArtifactUrlClassification classify(ClassPathClassifierContext context)
    {
        final File targetTestClassesFolder = new File(context.getTestClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        Predicate<MavenArtifact> exclusion = getExclusionsPredicate(context.getTestClass());

        MavenArtifact compileArtifact = getCompileArtifact(context.getMavenDependencies());
        logger.debug("Classification based on '{}'", compileArtifact);

        MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver = new DefaultMavenArtifactToClassPathURLResolver(context.getMavenMultiModuleArtifactMapping());

        List<URL> appURLs = buildAppUrls(context, artifactToClassPathURLResolver, exclusion, targetTestClassesFolder);
        List<PluginUrlClassification> pluginUrlClassifications = buildPluginsClassification(context, exclusion, compileArtifact, artifactToClassPathURLResolver, targetTestClassesFolder);
        List<URL> containerURLs = buildContainerUrls(context, appURLs, pluginUrlClassifications, artifactToClassPathURLResolver);

        return new ArtifactUrlClassification(containerURLs, pluginUrlClassifications, appURLs);
    }

    private MavenArtifact getCompileArtifact(final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies)
    {
        Optional<MavenArtifact> compileArtifact = allDependencies.keySet().stream().filter(artifact -> artifact.isCompileScope()).findFirst();
        if (!compileArtifact.isPresent())
        {
            throw new IllegalArgumentException("Couldn't get current artifactId mapped as compile in dependency graph, it should be the first compile dependency");
        }
        return compileArtifact.get();
    }

    private List<URL> buildAppUrls(final ClassPathClassifierContext context, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final Predicate<MavenArtifact> exclusion, final File targetTestClassesFolder)
    {
        // target/test-classes is not present in the dependency graph, so here is the only place were we must add it manually breaking the original order that came from class path
        List<URL> appURLs = context.getClassPathURLs().stream().filter(url -> url.getFile().equals(targetTestClassesFolder.getAbsolutePath() + File.separator)).collect(Collectors.toList());
        new DependencyResolver(new ConfigurationBuilder()
              .setMavenDependencyGraph(context.getMavenDependencies())
              .selectDependencies(
                      new DependenciesFilterBuilder()
                              .match(dependency -> dependency.isTestScope() && !exclusion.test(dependency))
              )
              .collectTransitiveDependencies(
                      new TransitiveDependenciesFilterBuilder()
                              .match(transitiveDependency -> transitiveDependency.isTestScope() && !exclusion.test(transitiveDependency))
                              .traverseWhenNoMatch()
              )).resolveDependencies().stream().filter(d -> !d.isPomType()).map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, context.getClassPathURLs())).collect(Collectors.toCollection(() -> appURLs));
        return appURLs;
    }

    private List<URL> buildContainerUrls(final ClassPathClassifierContext context, final List<URL> appURLs, final List<PluginUrlClassification> pluginUrlClassifications, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver)
    {
        // The container contains anything that is not application either extension class loader urls
        Set<URL> containerURLs = new LinkedHashSet<>();
        containerURLs.addAll(context.getClassPathURLs());
        containerURLs.removeAll(appURLs);
        pluginUrlClassifications.stream().forEach(pluginUrlClassification -> containerURLs.removeAll(pluginUrlClassification.getUrls()));

        // If a provided dependency was removed due to there is only one URL in class path for the same dependency, doesn't have the cardinality that maven has
        new DependencyResolver(new ConfigurationBuilder()
                                       .setMavenDependencyGraph(context.getMavenDependencies())
                                       .selectDependencies(new DependenciesFilterBuilder()
                                                                   .match(dependency -> dependency.isProvidedScope())
                                       )
                                       .collectTransitiveDependencies(
                                               new TransitiveDependenciesFilterBuilder()
                                                       .match(transitiveDependency -> transitiveDependency.isProvidedScope() || transitiveDependency.isCompileScope())
                                                       .traverseWhenNoMatch()
                                       )).resolveDependencies().stream().filter(d -> !d.isPomType()).map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, context.getClassPathURLs())).forEach(containerURLs::add);

        return newArrayList(containerURLs);
    }

    private List<PluginUrlClassification> buildPluginsClassification(final ClassPathClassifierContext context, final Predicate<MavenArtifact> exclusion, final MavenArtifact compileArtifact, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final File targetTestClassesFolder)
    {
        List<PluginUrlClassification> pluginClassifications = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
        String extensionsBasePackage = getAnnotationAttributeFrom(context.getTestClass(), ArtifactClassLoaderRunnerConfig.class, "extensionBasePackage");
        if (extensionsBasePackage != null && extensionsBasePackage.isEmpty())
        {
            throw new IllegalArgumentException("Base package for discovering extensions should be empty, it will take too much time to discover all the classes, please set a reasonable package for your extension annotate your tests with its base package");
        }
        Set<BeanDefinition> extensionsAnnotatedClasses = scanner.findCandidateComponents(extensionsBasePackage);

        boolean isRootArtifactIdAnExtensionToo = false;
        if (!extensionsAnnotatedClasses.isEmpty())
        {
            logger.debug("Extensions found, plugin class loaders would be created for each extension");
            Set<String> extensionsAnnotatedClassesNoDups = extensionsAnnotatedClasses.stream().map(bd -> bd.getBeanClassName()).collect(Collectors.toSet());
            for (String extensionClassName : extensionsAnnotatedClassesNoDups)
            {
                logger.debug("Classifying classpath for extension class: '{}'", extensionClassName);
                Class extensionClass;
                try
                {
                    extensionClass = forName(extensionClassName);
                }
                catch (ClassNotFoundException e)
                {
                    throw new IllegalArgumentException("Cannot create plugin/extension class loader classification due to extension class not found", e);
                }

                String extensionMavenArtifactId = getExtensionMavenArtifactId(extensionClass, context.getMavenMultiModuleArtifactMapping());
                isRootArtifactIdAnExtensionToo |= compileArtifact.getArtifactId().equals(extensionMavenArtifactId);

                pluginClassifications.add(extensionClassPathClassification(extensionClass, extensionMavenArtifactId, exclusion, artifactToClassPathURLResolver, context.getMavenDependencies(), compileArtifact, targetTestClassesFolder, context.getClassPathURLs()));
            }
        }

        if (!isRootArtifactIdAnExtensionToo)
        {
            logger.debug("Current maven artifact that holds the test class is not an extension, so a plugin class loader would be create with its compile dependencies");
            pluginClassifications.add(pluginClassPathClassification(exclusion, artifactToClassPathURLResolver, context.getMavenDependencies(), compileArtifact, targetTestClassesFolder, context.getClassPathURLs()));
        }
        return pluginClassifications;
    }

    /**
     * @return an {@link ExtensionManagerAdapter} that would be used to register the extensions, later it would be discarded.
     */
    private ExtensionManagerAdapter createExtensionManager()
    {
        DefaultExtensionManager extensionManager = new DefaultExtensionManager();
        extensionManager.setMuleContext(new DefaultMuleContext()
        {
            @Override
            public MuleRegistry getRegistry()
            {
                return new MuleRegistryHelper(new DefaultRegistryBroker(this), this);
            }
        });
        try
        {
            extensionManager.initialise();
        }
        catch (InitialisationException e)
        {
            throw new RuntimeException("Error while initialising the extension manager", e);
        }
        return extensionManager;
    }

    /**
     * Using the extensionClass and the location of the file source it will lookup for the maven artifact id using the
     * {@link MavenMultiModuleArtifactMapping}.
     *
     * @param extensionClass the class of the extension
     * @param mavenMultiModuleMapping the maven multi module mapping
     * @return the maven artifact id where the extension class belongs to
     */
    private String getExtensionMavenArtifactId(final Class extensionClass, final MavenMultiModuleArtifactMapping mavenMultiModuleMapping)
    {
        File extensionSourceCodeLocation = new File(extensionClass.getProtectionDomain().getCodeSource().getLocation().getPath());
        logger.debug("Extension: '{}' loaded from source path: '{}'", extensionClass.getName(), extensionSourceCodeLocation);
        // Just move up from jar/classes to the artifactId/multi-module folder
        File relativeFolder = extensionSourceCodeLocation.getParentFile();
        final StringBuilder extensionMavenArtifactId = new StringBuilder();
        // If it comes from a maven repository the parent folder shouldn't be "target"
        if (extensionSourceCodeLocation.isFile() && !relativeFolder.getName().equals(TARGET_FOLDER_NAME))
        {
            // It is a jar file, therefore the extension is not being tested as multi-module maven project
            extensionMavenArtifactId.append(relativeFolder.getParentFile().getName());
        }
        else
        {
            extensionMavenArtifactId.append(mavenMultiModuleMapping.getMavenArtifactIdFor(relativeFolder.getParentFile().getAbsolutePath() + File.separator));
        }

        return extensionMavenArtifactId.toString();
    }

    /**
     * It creates the resources for the given extension and does the classification of dependencies for the given extension and its artifactId in
     * order to collect the URLs to be used for the plugin {@link ClassLoader} for the extension.
     *
     * @param extension
     * @param extensionMavenArtifactId
     * @param exclusion
     * @param artifactToClassPathURLResolver
     * @param allDependencies
     * @param compileArtifact
     * @param targetTestClassesFolder
     * @param classPathURLs
     * @return the {@link PluginUrlClassification} for the given extension
     */
    private PluginUrlClassification extensionClassPathClassification(final Class extension, final String extensionMavenArtifactId, final Predicate<MavenArtifact> exclusion, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies, final MavenArtifact compileArtifact, final File targetTestClassesFolder, final List<URL> classPathURLs)
    {
        logger.debug("Classifying classpath for extension class : '{}', artifactId: '{}'", extension.getName(), extensionMavenArtifactId);
        List<URL> extensionURLs = new ArrayList<>();

        // First we need to add META-INF folder for generated resources due to they may be already created by another mvn install goal by the extension maven plugin
        File generatedResourcesDirectory = new File(targetTestClassesFolder.getParent(), GENERATED_TEST_SOURCES + File.separator + extensionMavenArtifactId + File.separator + "META-INF");
        generatedResourcesDirectory.mkdirs();
        ExtensionsTestInfrastructureDiscoverer extensionDiscoverer = new ExtensionsTestInfrastructureDiscoverer(createExtensionManager(), generatedResourcesDirectory);
        extensionDiscoverer.discoverExtensions(new Describer[0], new Class[] {extension});
        try
        {
            // Registering parent file as resource to be used from the configuration builder
            extensionURLs.add(generatedResourcesDirectory.getParentFile().toURI().toURL());
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Error while building resource URL for directory: " + generatedResourcesDirectory.getPath(), e);
        }

        int sizeBeforeDepResolver = extensionURLs.size();

        new DependencyResolver(new ConfigurationBuilder()
                                       .setMavenDependencyGraph(allDependencies)
                                       .includeRootArtifact(rootArtifact -> rootArtifact.getArtifactId().equals(extensionMavenArtifactId.toString()))
                                       .selectDependencies(
                                               new DependenciesFilterBuilder()
                                                       .match(dependency -> dependency.getArtifactId().equals(extensionMavenArtifactId.toString())
                                                                            || (compileArtifact.getArtifactId().equals(extensionMavenArtifactId.toString()) && dependency.isCompileScope() && !exclusion.test(dependency)))
                                       )
                                       .collectTransitiveDependencies(
                                               new TransitiveDependenciesFilterBuilder()
                                                       .match(transitiveDependency -> transitiveDependency.isCompileScope() && !exclusion.test(transitiveDependency))
                                       )).resolveDependencies().stream().filter(d -> !d.isPomType()).map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, classPathURLs)).forEach(extensionURLs::add);

        if (extensionURLs.size() == sizeBeforeDepResolver)
        {
            throw new IllegalStateException("There should be at least one compile dependency found that matched to extension: '" + extension.getName() +
                                            "'. Be aware that compile scope is what the classification uses for selecting the urls to be added to plugin class loader");
        }
        return new ExtensionPluginUrlClassification(extension, extensionURLs);
    }

    /**
     * Does the classification where the rootArtifact is not an extension so if it is not an extension it is treated as a plugin
     * where its compile dependencies and transitive dependencies should go to a plugin {@link ClassLoader}.
     *
     * @param exclusion
     * @param artifactToClassPathURLResolver
     * @param allDependencies
     * @param compileArtifact
     * @param targetTestClassesFolder
     * @param classPathURLs
     * @return
     */
    private PluginUrlClassification pluginClassPathClassification(final Predicate<MavenArtifact> exclusion, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies, final MavenArtifact compileArtifact, final File targetTestClassesFolder, final List<URL> classPathURLs)
    {
        Set<URL> urls = new DependencyResolver(new ConfigurationBuilder()
                                       .setMavenDependencyGraph(allDependencies)
                                       .includeRootArtifact()
                                       .selectDependencies(
                                               new DependenciesFilterBuilder()
                                                       .match(dependency -> dependency.isCompileScope() && !exclusion.test(dependency))
                                       )
                                       .collectTransitiveDependencies(
                                               new TransitiveDependenciesFilterBuilder()
                                                       .match(transitiveDependency -> transitiveDependency.isCompileScope() && !exclusion.test(transitiveDependency))
                                       )).resolveDependencies().stream().filter(d -> !d.isPomType()).map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, classPathURLs)).collect(Collectors.toSet());
        return new PluginUrlClassification(compileArtifact.getArtifactId(), Lists.newArrayList(urls));
    }

    /**
     * The list of exclusion GAT to be excluded from application/plugin classloaders due to these are supposed to
     * be exposed by the container.
     * It defined by the file excluded.properties and can be changed by having this file in the module that is tested or
     * appended to the default excluded GAT by marking the test with the annotation {@link ArtifactClassLoaderRunnerConfig}.
     *
     * @param klass
     * @return a {@link Predicate} to be used in order to excluded maven artifacts from application/plugin class loaders.
     */
    private Predicate<MavenArtifact> getExclusionsPredicate(final Class<?> klass)
    {
        Predicate<MavenArtifact> exclusionPredicate = null;
        try
        {
            Properties excludedProperties = RunnerModuleUtils.getExcludedProperties();
            String excludedModules = excludedProperties.getProperty("excluded.modules");
            if (excludedModules != null)
            {
                exclusionPredicate = createPredicate(exclusionPredicate, excludedModules);
            }
            else
            {
                logger.warn("excluded.properties found but there is no list of modules defined to be excluded, this could be the reason why the test may fail later due to JUnit classes are not found");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while loading excluded.properties file", e);
        }
        List<String> exclusionsAnnotated = getAnnotationAttributeFromHierarchy(klass, ArtifactClassLoaderRunnerConfig.class, "exclusions");
        for (String exclusionsToBeAppended : exclusionsAnnotated)
        {
            if (exclusionsToBeAppended != null && exclusionsToBeAppended.length() > 0)
            {
                exclusionPredicate = createPredicate(exclusionPredicate, exclusionsToBeAppended);
            }
        }

        return exclusionPredicate;
    }

    private Predicate<MavenArtifact> createPredicate(final Predicate<MavenArtifact> exclusionPredicate, final String exclusions)
    {
        Predicate<MavenArtifact> predicate = exclusionPredicate;
        for (String exclusion : exclusions.split(","))
        {
            String[] exclusionSplit = exclusion.split(":");
            if (exclusionSplit.length != 3)
            {
                throw new IllegalArgumentException("Exclusion pattern should be a GAT format, groupId:artifactId:type");
            }
            Predicate<MavenArtifact> artifactExclusion = new MavenArtifactMatcherPredicate(exclusionSplit[0], exclusionSplit[1], exclusionSplit[2]);
            if (predicate == null)
            {
                predicate = artifactExclusion;
            }
            else
            {
                predicate = predicate.or(artifactExclusion);
            }
        }
        return predicate;
    }
}
