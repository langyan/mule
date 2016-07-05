/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import static java.util.Arrays.stream;
import static org.mule.functional.util.AnnotationUtils.getAnnotationAttributeFrom;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig.DEFAULT;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfigs;
import org.mule.functional.junit4.runners.ArtifactClassloaderTestRunner;
import org.mule.functional.junit4.runners.ClassLoaderIsolatedExtensionsManagerConfigurationBuilder;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.api.config.ConfigurationBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.runner.RunWith;

/**
 * Base class for running {@link FunctionalTestCase} with class loader isolation that uses a {@link org.junit.runner.Runner}
 * that classifies the classpath provided by IDE/surfire-maven-plugin and classifies the URLs using the maven dependency graph.
 *
 * It has support for extensions and if the test class is annotated with {@link ArtifactClassLoaderRunnerConfig} and
 * extensions classes are defined.
 *
 * By default this test runs internally with a {@link org.junit.runners.JUnit4} runner.
 * For those cases where the test has to be run with another runner the {@link RunnerDelegateTo} should be used to define it.
 *
 * @since 4.0
 */
@RunWith(ArtifactClassloaderTestRunner.class)
public abstract class ArtifactFunctionalTestCase extends FunctionalTestCase
{
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

        ArtifactClassLoaderRunnerConfigs annotationGroup = this.getClass().getAnnotation(ArtifactClassLoaderRunnerConfigs.class);
        if (annotationGroup != null)
        {
            Set<Class> extensionsAnnotatedClasses = stream(annotationGroup.value()).filter(a -> !a.extension().equals(DEFAULT.class)).map(a -> a.extension()).collect(Collectors.toSet());
            builders.add(0, new ClassLoaderIsolatedExtensionsManagerConfigurationBuilder(extensionsAnnotatedClasses.toArray(new Class<?>[0])));
        }
    }

}
