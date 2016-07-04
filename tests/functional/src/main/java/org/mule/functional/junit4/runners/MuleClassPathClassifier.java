/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static java.util.Arrays.stream;
import static org.mule.functional.junit4.runners.AnnotationUtils.getAnnotationAttributeFrom;
import org.mule.functional.junit4.ExtensionsTestInfrastructureDiscoverer;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;

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
    public ArtifactUrlClassification classify(Class<?> klass, Set<URL> classPathURLs, LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies, MavenMultiModuleArtifactMapping mavenMultiModuleMapping)
    {
        final File targetTestClassesFolder = new File(klass.getProtectionDomain().getCodeSource().getLocation().getPath());

        final ClassLoaderURLsBuilder classLoaderURLsBuilder = new ClassLoaderURLsBuilder(classPathURLs, mavenMultiModuleMapping, allDependencies);
        Predicate<MavenArtifact> exclusion = getExclusionsPredicate(klass);

        MavenArtifact compileArtifact = getCompileArtifact(allDependencies);
        logger.debug("Classification based on: " + compileArtifact);

        Set<URL> appURLs = buildApplicationUrls(exclusion, classPathURLs, compileArtifact, classLoaderURLsBuilder, targetTestClassesFolder);
        List<Set<URL>> extensionsURLs = buildExtensionsUrls(klass, exclusion, compileArtifact, classLoaderURLsBuilder, mavenMultiModuleMapping, targetTestClassesFolder);
        Set<URL> containerURLs = buildContainerUrls(classPathURLs, appURLs, extensionsURLs, compileArtifact, classLoaderURLsBuilder);

        return new ArtifactUrlClassification(containerURLs, extensionsURLs, appURLs);
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

    protected Set<URL> buildApplicationUrls(Predicate<MavenArtifact> exclusion, Set<URL> classPathURLs, MavenArtifact compileArtifact, ClassLoaderURLsBuilder classLoaderURLsBuilder, File targetTestClassesFolder)
    {
        boolean shouldAddOnlyDependencies = true;
        boolean shouldAddTransitiveDepFromExclude = true;

        Predicate<MavenArtifact> onlyTheCompileArtifact = artifact -> artifact.equals(compileArtifact);
        Predicate<MavenArtifact> onlyTestArtifactsNotExcluded = artifact -> artifact.isTestScope() && !exclusion.test(artifact);

        Set<URL> appURLs = classLoaderURLsBuilder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExclude, onlyTheCompileArtifact, onlyTestArtifactsNotExcluded);
        // Plus the target/test-classes of the current compiled artifact
        appURLs.addAll(classPathURLs.stream().filter(url -> url.getFile().trim().equals(targetTestClassesFolder.getAbsolutePath() + File.separator)).collect(Collectors.toSet()));

        return appURLs;
    }

    protected List<Set<URL>> buildExtensionsUrls(Class<?> klass, Predicate<MavenArtifact> exclusion, MavenArtifact compileArtifact, ClassLoaderURLsBuilder classLoaderURLsBuilder, MavenMultiModuleArtifactMapping mavenMultiModuleMapping, File targetTestClassesFolder)
    {
        Class[] extensions = getExtensions(klass);

        List<Set<URL>> extensionsURLs = new ArrayList<>(extensions.length);
        stream(extensions).forEach(extension -> extensionsURLs.add(extensionClassPathClassification(extension, exclusion, mavenMultiModuleMapping, classLoaderURLsBuilder, compileArtifact, targetTestClassesFolder)));
        return extensionsURLs;
    }

    protected Set<URL> buildContainerUrls(Set<URL> classPathURLs, Set<URL> appURLs, List<Set<URL>> extensionsURLs, MavenArtifact compileArtifact, ClassLoaderURLsBuilder classLoaderURLsBuilder)
    {
        // The container contains anything that is not application either extension classloader urls
        Set<URL> containerURLs = new HashSet<>();
        containerURLs.addAll(classPathURLs);
        containerURLs.removeAll(appURLs);

        extensionsURLs.stream().forEach(eURLs -> containerURLs.removeAll(eURLs));

        boolean shouldAddOnlyDependencies = true;
        boolean shouldAddTransitiveDepFromExclude = false;

        Predicate<MavenArtifact> onlyTheCompileArtifact = artifact -> artifact.equals(compileArtifact);
        Predicate<MavenArtifact> onlyProvidedAndCompileArtifacts = artifact -> artifact.isProvidedScope() || artifact.isCompileScope();

        // After removing all the plugin and application urls we add provided dependencies urls (supports for having same dependencies as provided transitive and compile either test)
        Set<URL> containerProvidedDependenciesURLs = classLoaderURLsBuilder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExclude, onlyTheCompileArtifact, onlyProvidedAndCompileArtifacts);
        containerURLs.addAll(containerProvidedDependenciesURLs);

        return containerURLs;
    }

    private Set<URL> extensionClassPathClassification(Class<?> extension, Predicate<MavenArtifact> exclusion, MavenMultiModuleArtifactMapping mavenMultiModuleMapping, ClassLoaderURLsBuilder classLoaderURLsBuilder, MavenArtifact compileArtifact, File targetTestClassesFolder)
    {
        Set<URL> extensionURLs = new LinkedHashSet<>();
        String extensionMavenArtifactId = mavenMultiModuleMapping.getMavenArtifactIdFor(extension);

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

        // Just get the extension maven artifact without its dependencies (case if the extension maven artifact doesn't have dependencies)
        extensionURLs.addAll(classLoaderURLsBuilder.buildClassLoaderURLs(!compileArtifact.getArtifactId().equals(extensionMavenArtifactId), false, artifact -> artifact.equals(compileArtifact), dependency -> dependency.getArtifactId().equals(extensionMavenArtifactId)));
        // Get dependencies from the extension maven artifact
        extensionURLs.addAll(classLoaderURLsBuilder.buildClassLoaderURLs(true, false, artifact -> artifact.getArtifactId().equals(extensionMavenArtifactId), dep -> dep.isCompileScope() && !exclusion.test(dep)));

        return extensionURLs;
    }

    private Class[] getExtensions(Class<?> klass)
    {
        return getAnnotationAttributeFrom(klass, ArtifactClassLoaderRunnerConfig.class, "extensions");
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
    private Predicate<MavenArtifact> getExclusionsPredicate(Class<?> klass)
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

    private Predicate<MavenArtifact> createPredicate(Predicate<MavenArtifact> exclusionPredicate, String exclusions)
    {
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
                exclusionPredicate = artifactExclusion;
            }
            else
            {
                exclusionPredicate = exclusionPredicate.or(artifactExclusion);
            }
        }
        return exclusionPredicate;
    }
}
