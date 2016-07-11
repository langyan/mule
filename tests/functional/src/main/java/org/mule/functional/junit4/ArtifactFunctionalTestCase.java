/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import org.mule.functional.classloading.isolation.builder.IsolatedClassLoaderExtensionsManagerConfigurationBuilder;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunner;
import org.mule.functional.junit4.runners.PluginClassLoadersAware;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.util.List;

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
@RunWith(ArtifactClassLoaderRunner.class)
public abstract class ArtifactFunctionalTestCase extends FunctionalTestCase
{
    private static List<ArtifactClassLoader> pluginClassLoaders;

    /**
     * @return the TCCL as it will be the application {@link ClassLoader} created by the runner.
     */
    @Override
    protected ClassLoader getExecutionClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }

    @PluginClassLoadersAware
    public static void setPluginClassLoaders(List<ArtifactClassLoader> artifactClassLoaders)
    {
        pluginClassLoaders = artifactClassLoaders;
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        super.addBuilders(builders);
        Class<?> runner = getAnnotationAttributeFrom(this.getClass(), RunWith.class, "value");
        if (runner == null || !runner.equals(ArtifactClassLoaderRunner.class))
        {
            throw new IllegalStateException(this.getClass().getName() + " extends " + ArtifactFunctionalTestCase.class.getName()
                                            + " so it should be annotated to only run with: " + ArtifactClassLoaderRunner.class + ". See " + RunnerDelegateTo.class + " for defining a delegate runner to be used.");
        }

        if (pluginClassLoaders != null && !pluginClassLoaders.isEmpty())
        {
            builders.add(0, new IsolatedClassLoaderExtensionsManagerConfigurationBuilder(pluginClassLoaders));
        }
    }

}
