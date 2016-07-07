/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFromHierarchy;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.functional.junit4.runners.PluginClassLoadersAware;
import org.mule.functional.junit4.runners.ArtifactClassloaderTestRunner;
import org.mule.functional.classloading.isolation.builder.ClassLoaderIsolatedExtensionsManagerConfigurationBuilder;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.runner.RunWith;

/**
 * Base class for running {@link FunctionalTestCase} with class loader isolation that uses a {@link org.junit.runner.Runner}
 * that classifies the classpath provided by IDE/surfire-maven-plugin and classifies the URLs using the maven dependency graph.
 * <p/>
 * It has support for extensions and if the test class is annotated with {@link ArtifactClassLoaderRunnerConfig} and
 * extensions classes are defined.
 * <p/>
 * By default this test runs internally with a {@link org.junit.runners.JUnit4} runner.
 * For those cases where the test has to be run with another runner the {@link RunnerDelegateTo} should be used to define it.
 *
 * @since 4.0
 */
@RunWith(ArtifactClassloaderTestRunner.class)
public abstract class ArtifactFunctionalTestCase extends FunctionalTestCase
{
    private static List<ArtifactClassLoader> extensionClassLoaders;

    @PluginClassLoadersAware
    public static void setExtensionClassLoaders(List<ArtifactClassLoader> pluginClassLoaders)
    {
        extensionClassLoaders = pluginClassLoaders;
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);
        Class<?> runner = getAnnotationAttributeFrom(this.getClass(), RunWith.class, "value");
        if (runner == null || !runner.equals(ArtifactClassloaderTestRunner.class))
        {
            throw new IllegalStateException(this.getClass().getName() + " extends " + ArtifactFunctionalTestCase.class.getName()
                                            + " so it should be annotated to only run with: " + ArtifactClassloaderTestRunner.class + ". See " + RunnerDelegateTo.class + " for defining a delegate runner to be used.");
        }

        List<String[]> extensionsAnnotatedClasses = getAnnotationAttributeFromHierarchy(this.getClass(), ArtifactClassLoaderRunnerConfig.class, "extensions");
        if (!extensionsAnnotatedClasses.isEmpty())
        {
            Set<String> extensionsAnnotatedClassesNoDups = extensionsAnnotatedClasses.stream().flatMap(Arrays::stream).collect(Collectors.toSet());
            builders.add(0, new ClassLoaderIsolatedExtensionsManagerConfigurationBuilder(extensionsAnnotatedClassesNoDups.toArray(new String[0]), extensionClassLoaders));
        }
    }

}
