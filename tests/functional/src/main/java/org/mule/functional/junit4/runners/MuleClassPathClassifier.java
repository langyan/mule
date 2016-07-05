/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static com.google.common.collect.Lists.newArrayList;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import org.mule.functional.junit4.ExtensionsTestInfrastructureDiscoverer;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;

import com.google.common.collect.Lists;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
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

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ArtifactUrlClassification classify(ClassPathClassifierContext context)
    {
        final File targetTestClassesFolder = new File(context.getTestClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        Predicate<MavenArtifact> exclusion = getExclusionsPredicate(context.getTestClass());

        MavenArtifact compileArtifact = getCompileArtifact(context.getMavenDependencies());
        logger.debug("Classification based on: " + compileArtifact);

        MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver = new DefaultMavenArtifactToClassPathURLResolver(context.getMavenMultiModuleArtifactMapping());

        List<URL> appURLs = buildAppUrls(context, artifactToClassPathURLResolver, exclusion, targetTestClassesFolder);
        List<PluginUrlClassification> pluginUrlClassifications = buildExtensionsClassification(context, exclusion, compileArtifact, artifactToClassPathURLResolver, targetTestClassesFolder);
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
        Set<URL> appURLs = new DependencyResolver(new ConfigurationBuilder()
                                                          .setMavenDependencyGraph(context.getMavenDependencies())
                                                          .selectDependencies(
                                                                  new DependenciesFilterBuilder()
                                                                          .match(dependency -> dependency.isTestScope() && !exclusion.test(dependency))
                                                          )
                                                          .collectTransitiveDependencies(
                                                                  new TransitiveDependenciesFilterBuilder()
                                                                          .match(transitiveDependency -> transitiveDependency.isTestScope() && !exclusion.test(transitiveDependency))
                                                                          .includeTransitiveDependenciesFromFiltered()
                                                          )).resolveDependencies().stream().map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, context.getClassPathURLs())).collect(Collectors.toSet());
        // Plus the target/test-classes of the current compiled artifact
        appURLs.addAll(context.getClassPathURLs().stream().filter(url -> url.getFile().trim().equals(targetTestClassesFolder.getAbsolutePath() + File.separator)).collect(Collectors.toSet()));
        return Lists.newArrayList(appURLs);
    }

    private List<URL> buildContainerUrls(final ClassPathClassifierContext context, final List<URL> appURLs, final List<PluginUrlClassification> pluginUrlClassifications, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver)
    {
        // The container contains anything that is not application either extension classloader urls
        Set<URL> containerURLs = new HashSet<>();
        containerURLs.addAll(context.getClassPathURLs());
        containerURLs.removeAll(appURLs);
        pluginUrlClassifications.stream().forEach(pluginUrlClassification -> containerURLs.removeAll(pluginUrlClassification.getUrls()));

        new DependencyResolver(new ConfigurationBuilder()
                                       .setMavenDependencyGraph(context.getMavenDependencies())
                                       .selectDependencies(new DependenciesFilterBuilder()
                                                                   .match(dependency -> dependency.isProvidedScope())
                                       )
                                       .collectTransitiveDependencies(
                                               new TransitiveDependenciesFilterBuilder()
                                                       .match(transitiveDependency -> transitiveDependency.isProvidedScope() || transitiveDependency.isCompileScope())
                                                       .includeTransitiveDependenciesFromFiltered()
                                       )).resolveDependencies().stream().map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, context.getClassPathURLs())).forEach(containerURLs::add);

        return newArrayList(containerURLs);
    }

    private List<PluginUrlClassification> buildExtensionsClassification(final ClassPathClassifierContext context, final Predicate<MavenArtifact> exclusion, final MavenArtifact compileArtifact, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final File targetTestClassesFolder)
    {
        List<PluginUrlClassification> pluginClassifications = new ArrayList<>();
        ArtifactClassLoaderRunnerConfig[] annotations = context.getTestClass().getAnnotationsByType(ArtifactClassLoaderRunnerConfig.class);
        for(ArtifactClassLoaderRunnerConfig annotation : annotations)
        {
            Class extension = annotation.extension();
            if (!extension.equals(ArtifactClassLoaderRunnerConfig.DEFAULT.class))
            {
                pluginClassifications.add(extensionClassPathClassification(extension, exclusion, context.getMavenMultiModuleArtifactMapping(), artifactToClassPathURLResolver, context.getMavenDependencies(), compileArtifact, targetTestClassesFolder, context.getClassPathURLs()));
            }
        }
        return pluginClassifications;
    }

    private PluginUrlClassification extensionClassPathClassification(final Class<?> extension, final Predicate<MavenArtifact> exclusion, final MavenMultiModuleArtifactMapping mavenMultiModuleMapping, final MavenArtifactToClassPathURLResolver artifactToClassPathURLResolver, final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies, final MavenArtifact compileArtifact, final File targetTestClassesFolder, final List<URL> classPathURLs)
    {
        logger.debug("Classifying classpath for extension class: " + extension.getName());
        Set<URL> extensionURLs = new LinkedHashSet<>();
        File extensionSourceCodeLocation = new File(extension.getProtectionDomain().getCodeSource().getLocation().getPath());
        // Just move up from jar/classes to the artifactId/multi-module folder
        File relativeFolder = extensionSourceCodeLocation.getParentFile().getParentFile();
        final StringBuilder extensionMavenArtifactId = new StringBuilder();
        if (extensionSourceCodeLocation.isFile())
        {
            // It is a jar file, therefore the extension is not being tested as multi-module maven project
            extensionMavenArtifactId.append(relativeFolder.getName());
        }
        else
        {
            extensionMavenArtifactId.append(mavenMultiModuleMapping.getMavenArtifactIdFor(relativeFolder.getAbsolutePath() + File.separator));
        }

        // First we need to add META-INF folder for generated resources due to they may be already created by another mvn install goal by the extension maven plugin
        File generatedResourcesDirectory = new File(targetTestClassesFolder.getParent(), GENERATED_TEST_SOURCES + File.separator + extensionMavenArtifactId + File.separator + "META-INF");
        generatedResourcesDirectory.mkdirs();
        ExtensionsTestInfrastructureDiscoverer extensionDiscoverer = new ExtensionsTestInfrastructureDiscoverer(generatedResourcesDirectory);
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

        new DependencyResolver(new ConfigurationBuilder()
                                       .setMavenDependencyGraph(allDependencies)
                                       .includeRootArtifactInResults(rootArtifact -> rootArtifact.getArtifactId().equals(extensionMavenArtifactId.toString()))
                                       .selectDependencies(
                                               new DependenciesFilterBuilder()
                                                       .match(dependency -> dependency.getArtifactId().equals(extensionMavenArtifactId.toString())
                                                                            || (compileArtifact.getArtifactId().equals(extensionMavenArtifactId.toString()) && dependency.isCompileScope() && !exclusion.test(dependency)))
                                       )
                                       .collectTransitiveDependencies(
                                               new TransitiveDependenciesFilterBuilder()
                                                       .match(transitiveDependency -> transitiveDependency.isCompileScope() && !exclusion.test(transitiveDependency))
                                       )).resolveDependencies().stream().map(dependency -> artifactToClassPathURLResolver.resolveURL(dependency, classPathURLs)).forEach(extensionURLs::add);

        return new PluginUrlClassification(extension, newArrayList(extensionURLs));
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
        String exclusionsToBeAppended = getAnnotationAttributeFrom(klass, ArtifactClassLoaderRunnerConfig.class, "exclusions");
        if (exclusionsToBeAppended != null && exclusionsToBeAppended.length() > 0)
        {
            exclusionPredicate = createPredicate(exclusionPredicate, exclusionsToBeAppended);
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
            if (exclusionPredicate == null)
            {
                predicate = artifactExclusion;
            }
            else
            {
                predicate = exclusionPredicate.or(artifactExclusion);
            }
        }
        return predicate;
    }
}
