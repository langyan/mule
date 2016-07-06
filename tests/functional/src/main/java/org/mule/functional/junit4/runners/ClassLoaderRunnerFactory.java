/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

/**
 * Creates a {@link ClassLoader} to be used for running the JUnit test.
 *
 * @since 4.0
 */
public interface ClassLoaderRunnerFactory
{

    /**
     * @param testClass the test class about to be executed in order to allow getting its annotations
     * @param artifactUrlClassification an URL classification that defines the set of {@link java.net.URL} to be used for building the classloader
     * @return the {@link ClassLoaderTestRunner} to be used for running the test.
     */
    ClassLoaderTestRunner createClassLoader(Class<?> testClass, ArtifactUrlClassification artifactUrlClassification);
}
