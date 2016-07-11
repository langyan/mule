/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classloader;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static java.util.Collections.addAll;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFromHierarchy;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.functional.classloading.isolation.classification.ArtifactUrlClassification;
import org.mule.functional.classloading.isolation.classification.ClassLoaderTestRunner;
import org.mule.functional.classloading.isolation.classification.PluginUrlClassification;
import org.mule.functional.classloading.isolation.utils.RunnerModuleUtils;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.MuleModule;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.CompositeClassLoader;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;

import com.google.common.collect.Sets;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates that mimics the class loading hierarchy used in a standalone container.
 * <p/>
 * The class loaders created have the following hierarchy:
 * <p/>
 * <ul>
 * <li>Container: all the provided scope dependencies plus their dependencies (if they are not test) and java</li>
 * <li>Plugins (optional): for each plugin a class loader will be created with all the compile scope dependencies and their dependencies (only the ones with scope compile)</li>
 * <li>Application: all the test scope dependencies and their dependencies if they are not defined to be excluded, plus the test dependencies
 * from the compile scope dependencies (again if they are not excluded).</li>
 * </ul>
 *
 * @since 4.0
 */
public class MuleClassLoaderRunnerFactory
{

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());
    private ArtifactClassLoaderFilterFactory artifactClassLoaderFilterFactory = new DefaultArtifactClassLoaderFilterFactory();
    private DefaultExtensionManager extensionManager = new DefaultExtensionManager();

    /**
     * Creates a {@link ClassLoaderTestRunner} containing the container, plugins and application {@link ArtifactClassLoader}s
     * @param klass
     * @param artifactUrlClassification
     * @return a {@link ClassLoaderTestRunner}
     */
    public ClassLoaderTestRunner createClassLoader(Class<?> klass, ArtifactUrlClassification artifactUrlClassification)
    {
        final TestContainerClassLoaderFactory testContainerClassLoaderFactory = new TestContainerClassLoaderFactory(getExtraBootPackages(klass), artifactUrlClassification.getContainerURLs().toArray(new URL[0]));

        ArtifactClassLoader containerClassLoader = createContainerArtifactClassLoader(testContainerClassLoaderFactory, artifactUrlClassification);
        ClassLoader classLoader = containerClassLoader.getClassLoader();

        ClassLoaderLookupPolicy childClassLoaderLookupPolicy = testContainerClassLoaderFactory.getContainerClassLoaderLookupPolicy();

        List<ArtifactClassLoader> pluginsArtifactClassLoaders = new ArrayList<>();
        if (!artifactUrlClassification.getPluginClassificationURLs().isEmpty())
        {
            classLoader = createPluginClassLoaders(classLoader, childClassLoaderLookupPolicy, artifactUrlClassification, pluginsArtifactClassLoaders);
        }

        ArtifactClassLoader appClassLoader = createApplicationArtifactClassLoader(classLoader, childClassLoaderLookupPolicy, artifactUrlClassification);

        return new ClassLoaderTestRunner(containerClassLoader, pluginsArtifactClassLoaders, appClassLoader);
    }

    /**
     * Creates an {@link ArtifactClassLoader} for the container. As difference from a mule container this one has to be aware that the parent class loader
     * has all the URLs loaded in launcher app class loader so it has to create a particular look policy to resolve classes as CHILD_FIRST.
     * In order to do that a {@link FilteringArtifactClassLoader} is created to similates the launcher class loader to resolve only classes from the URLs defined
     * to be in the container.
     *
     * @param testContainerClassLoaderFactory {@link TestContainerClassLoaderFactory} that has the logic to create a container class loader
     * @param artifactUrlClassification the url classifications to get plugins urls
     * @return an {@link ArtifactClassLoader} for the container.
     */
    private ArtifactClassLoader createContainerArtifactClassLoader(TestContainerClassLoaderFactory testContainerClassLoaderFactory, ArtifactUrlClassification artifactUrlClassification)
    {
        logClassLoaderUrls("CONTAINER", artifactUrlClassification.getContainerURLs());
        MuleArtifactClassLoader launcherArtifact = new MuleArtifactClassLoader("launcher", new URL[0],
                                                                               MuleClassLoaderRunnerFactory.class.getClassLoader(), new MuleClassLoaderLookupPolicy(Collections.emptyMap(), Collections.<String>emptySet()));
        ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory().create(testContainerClassLoaderFactory.getBootPackages(), Collections.<MuleModule>emptyList());

        return testContainerClassLoaderFactory.createContainerClassLoader(new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher));
    }

    /**
     * For each plugin defined in the classification it will create an {@link ArtifactClassLoader} with the name defined in classification.
     * For extension plugins it will also create the filter based on the extension manifest file. For a plain plugin it will collect the exported
     * packages and resources for creating the filter from its mule-module.properties file.
     * <pr/>
     * It also creates a sharedLibs plugin without any library so far, in order to be a similar representation as mule's container has.
     * <pr/>
     * With the given list of created class loader plugins it will finally create a {@link CompositeClassLoader} and return it.
     *
     * @param parent the parent class loader to be assigned to the new one created here
     * @param childClassLoaderLookupPolicy look policy to be used
     * @param artifactUrlClassification the url classifications to get plugins urls
     * @param pluginsArtifactClassLoaders a list where it would append each {@link ArtifactClassLoader} created for a plugin in order to allow access them later
     * @return a {@link CompositeClassLoader} that represents the plugin class loaders.
     */
    private ClassLoader createPluginClassLoaders(ClassLoader parent, ClassLoaderLookupPolicy childClassLoaderLookupPolicy, ArtifactUrlClassification artifactUrlClassification, List<ArtifactClassLoader> pluginsArtifactClassLoaders)
    {
        final List<ClassLoader> pluginClassLoaders = new ArrayList<>();
        pluginClassLoaders.add(new MuleArtifactClassLoader("sharedLibs", new URL[0], parent, childClassLoaderLookupPolicy));

        for (PluginUrlClassification pluginUrlClassification : artifactUrlClassification.getPluginClassificationURLs())
        {
            logClassLoaderUrls("PLUGIN (" + pluginUrlClassification.getName() + ")", pluginUrlClassification.getUrls());
            MuleArtifactClassLoader pluginCL = new MuleArtifactClassLoader(pluginUrlClassification.getName(), pluginUrlClassification.getUrls().toArray(new URL[0]), parent, childClassLoaderLookupPolicy);
            pluginsArtifactClassLoaders.add(pluginCL);

            Collection<String> exportedPackages;
            Collection<String> exportedResources;
            URL manifestUrl = pluginCL.findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
            if (manifestUrl != null)
            {
                logger.debug("Plugin '{}' has extension descriptor therefore it will be handled as an extension", pluginUrlClassification.getName());
                ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
                exportedPackages = extensionManifest.getExportedPackages();
                exportedResources = extensionManifest.getExportedResources();
            }
            else
            {
                logger.debug("Plugin '{}' will be handled as standard plugin, it is not an extension", pluginUrlClassification.getName());
                ClassLoader pluginArtifactClassLoaderToDiscoverModules = new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null);
                List<MuleModule> modules = withContextClassLoader(pluginArtifactClassLoaderToDiscoverModules, () -> new ClasspathModuleDiscoverer(pluginArtifactClassLoaderToDiscoverModules).discover());
                MuleModule module = validatePluginModule(pluginUrlClassification.getName(), modules);

                exportedPackages = module.getExportedPackages();
                exportedResources = module.getExportedPackages();
            }
            ArtifactClassLoaderFilter filter = artifactClassLoaderFilterFactory.create(exportedPackages.stream().collect(Collectors.joining(", ")),
                                                                                       exportedResources.stream().collect(Collectors.joining(", ")));
            pluginClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter));
        }
        return new CompositeClassLoader(parent, pluginClassLoaders, childClassLoaderLookupPolicy);
    }

    /**
     * Validates that only one module should be discovered.
     *
     * @param pluginName
     * @param discoveredModules
     * @return the first Module from the list due to there should be only one module.
     */
    private MuleModule validatePluginModule(String pluginName, List<MuleModule> discoveredModules)
    {
        if (discoveredModules.size() == 0)
        {
            throw new IllegalStateException(pluginName + " doesn't have in its classpath a mule-module.properties to define what packages and resources should expose");
        }
        if (discoveredModules.size() > 1)
        {
            throw new IllegalStateException(pluginName + " has more than one mule-module.properties, composing plugins is not allowed");
        }
        return discoveredModules.get(0);
    }

    /**
     * Creates an {@link ArtifactClassLoader} for the application.
     *
     * @param parent the parent class loader to be assigned to the new one created here
     * @param childClassLoaderLookupPolicy look policy to be used
     * @param artifactUrlClassification the url classifications to get plugins urls
     * @return the {@link ArtifactClassLoader} to be used for test runner as in TCCL.
     */
    private ArtifactClassLoader createApplicationArtifactClassLoader(ClassLoader parent, ClassLoaderLookupPolicy childClassLoaderLookupPolicy, ArtifactUrlClassification artifactUrlClassification)
    {
        logClassLoaderUrls("APP", artifactUrlClassification.getApplicationURLs());
        return new MuleArtifactClassLoader("app", artifactUrlClassification.getApplicationURLs().toArray(new URL[0]), parent, childClassLoaderLookupPolicy);
    }

    private void logClassLoaderUrls(final String classLoaderName, final List<URL> urls)
    {
        StringBuilder builder = new StringBuilder(classLoaderName).append(" classloader urls: [");
        urls.stream().forEach(e -> builder.append("\n").append(" ").append(e.getFile()));
        builder.append("\n]");
        logClassLoadingTrace(builder.toString());
    }

    private void logClassLoadingTrace(String message)
    {
        if (isVerboseClassLoading())
        {
            logger.info(message);
        }
        else if (logger.isTraceEnabled())
        {
            logger.trace(message);
        }
    }

    private Boolean isVerboseClassLoading()
    {
        return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
    }

    private Set<String> getExtraBootPackages(Class<?> klass)
    {
        Set<String> packages = Sets.newHashSet();

        List<String> extraBootPackagesList = getAnnotationAttributeFromHierarchy(klass, ArtifactClassLoaderRunnerConfig.class, "extraBootPackages");
        extraBootPackagesList.stream().filter(extraBootPackages -> !isEmpty(extraBootPackages)).forEach(extraBootPackages -> addAll(packages, extraBootPackages.split(",")));

        // Add default boot package always, they are defined in excluded.properties file!
        try
        {
            Properties excludedProperties = RunnerModuleUtils.getExcludedProperties();
            String excludedExtraBootPackages = excludedProperties.getProperty("extraBoot.packages");
            if (excludedExtraBootPackages != null)
            {
                for (String extraBootPackage : excludedExtraBootPackages.split(","))
                {
                    packages.add(extraBootPackage);
                }
            }
            else
            {
                logger.warn("excluded.properties found but there is no list of extra boot packages defined to be added to container, this could be the reason why the test may fail later due to JUnit classes are not found");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while loading excluded.properties file", e);
        }
        return packages;
    }

}
