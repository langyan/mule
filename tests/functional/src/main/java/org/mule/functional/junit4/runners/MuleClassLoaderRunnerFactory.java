/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.container.internal.ContainerClassLoaderFilterFactory;
import org.mule.runtime.container.internal.MuleModule;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Mule implementation that creates almost the same classloader hierarchy that is used by Mule when running
 * applications.
 * The classloaders created have the following hierarchy:
 * <ul>
 * <li>Container: all the provided scope dependencies plus their dependencies (if they are not test) and java</li>
 * <li>Plugin (optional): all the compile scope dependencies and their dependencies (only the ones with scope compile)</li>
 * <li>Application: all the test scope dependencies and their dependencies if they are not defined to be excluded, plus the test dependencies
 * from the compile scope dependencies (again if they are not excluded).</li>
 * </ul>
 *
 * @since 4.0
 */
public class MuleClassLoaderRunnerFactory implements ClassLoaderRunnerFactory
{

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());
    private ArtifactClassLoaderFilterFactory artifactClassLoaderFilterFactory = new DefaultArtifactClassLoaderFilterFactory();
    private DefaultExtensionManager extensionManager = new DefaultExtensionManager();

    @Override
    public ClassLoader createClassLoader(Class<?> klass, ArtifactUrlClassification artifactUrlClassification)
    {
        // Container classLoader
        logClassLoaderUrls("CONTAINER", artifactUrlClassification.getContainer());
        final TestContainerClassLoaderFactory testContainerClassLoaderFactory = new TestContainerClassLoaderFactory(getExtraBootPackages(klass), artifactUrlClassification.getContainer().toArray(new URL[0]));

        MuleArtifactClassLoader launcherArtifact = new MuleArtifactClassLoader("launcher", new URL[0],
                                                                               MuleClassLoaderRunnerFactory.class.getClassLoader(), new MuleClassLoaderLookupPolicy(Collections.emptyMap(), Collections.<String>emptySet()));
        ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory().create(testContainerClassLoaderFactory.getBootPackages(), Collections.<MuleModule>emptyList());

        ClassLoader classLoader = testContainerClassLoaderFactory.createContainerClassLoader(new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher)).getClassLoader();

        ClassLoaderLookupPolicy childClassLoaderLookupPolicy = testContainerClassLoaderFactory.getContainerClassLoaderLookupPolicy(classLoader);

        // Plugin classloaders
        if (!artifactUrlClassification.getPlugins().isEmpty())
        {
            final List<ClassLoader> pluginClassLoaders = new ArrayList<>();
            pluginClassLoaders.add(new MuleArtifactClassLoader("sharedLibs", new URL[0], classLoader, childClassLoaderLookupPolicy));

            for (List<URL> pluginUrls : artifactUrlClassification.getPlugins())
            {
                // Plugin classLoader
                logClassLoaderUrls("PLUGIN", pluginUrls);
                MuleArtifactClassLoader pluginCL = new MuleArtifactClassLoader("plugin", pluginUrls.toArray(new URL[0]), classLoader, childClassLoaderLookupPolicy);

                URL manifestUrl = pluginCL.findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
                ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
                ArtifactClassLoaderFilter filter = artifactClassLoaderFilterFactory.create(extensionManifest.getExportedPackages().stream().collect(Collectors.joining(", ")), extensionManifest.getExportedResources().stream().collect(Collectors.joining(", ")));
                pluginClassLoaders.add(new FilteringArtifactClassLoader(pluginCL, filter));
            }
            classLoader = new CompositeClassLoader(classLoader, pluginClassLoaders, childClassLoaderLookupPolicy);
        }

        // Application classLoader
        logClassLoaderUrls("APP", artifactUrlClassification.getApplication());
        classLoader = new MuleArtifactClassLoader("app", artifactUrlClassification.getApplication().toArray(new URL[0]), classLoader, childClassLoaderLookupPolicy);

        return classLoader;
    }

    private void logClassLoaderUrls(final String classLoaderName, final List<URL> urls)
    {
        //TODO add system property!
        if (logger.isDebugEnabled())
        {
            StringBuilder builder = new StringBuilder(classLoaderName).append(" classloader urls: [");
            urls.stream().forEach(e -> builder.append("\n").append(" ").append(e.getFile()));
            builder.append("\n]");
            logger.debug(builder.toString());
        }
    }

    private Set<String> getExtraBootPackages(Class<?> klass)
    {
        String extraPackages = getAnnotationAttributeFrom(klass, ArtifactClassLoaderRunnerConfig.class, "extraBootPackages");

        Set<String> packages = Sets.newHashSet(extraPackages.split(","));

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
