/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import org.junit.internal.runners.statements.Fail;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * TODO this should be the new Artifact, add support for delegate
 */
public class ClassLoaderPerTestRunner extends BlockJUnit4ClassRunner
{
    /**
     * Instantiates a new test per class loader runner.
     *
     * @param klass the class
     * @throws InitializationError the initialization error
     */
    public ClassLoaderPerTestRunner(Class<?> klass)
            throws InitializationError
    {
        super(createTestClassUsingClassLoader(klass));
    }

    /**
     * Creates the isolated {@link ClassLoader} that would be used to run the Test.
     * A {@link ClassLoader} would be created by Test class.
     * @param klass
     * @return the test class loaded with the isolated {@link ClassLoader}
     * @throws InitializationError
     */
    //TODO(gsfernandes) see the impact of having to create a classloader for test class vs what Runner docs says:
    //" * The default runner implementation guarantees that the instances of the test case\n"+
    //        " * class will be constructed immediately before running the test and that the runner\n"+
    //        " * will retain no reference to the test case instances, generally making them\n"+
    //        " * available for garbage collection."
    private static Class<?> createTestClassUsingClassLoader(Class<?> klass) throws InitializationError
    {
        // Initializes utility classes
        ClassPathURLsProvider classPathURLsProvider = new DefaultClassPathURLsProvider();
        MavenDependenciesResolver mavenDependenciesResolver = new DependencyGraphMavenDependenciesResolver();
        MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new MuleMavenMultiModuleArtifactMapping();
        ClassLoaderRunnerFactory classLoaderRunnerFactory = new MuleClassLoaderRunnerFactory();
        ClassPathClassifier classPathClassifier = new MuleClassPathClassifier();

        // Does the classification and creation of the isolated ClassLoader
        ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(klass, classPathURLsProvider.getURLs(),
                                                                                           mavenDependenciesResolver.buildDependencies(), mavenMultiModuleArtifactMapping);
        ClassLoader isolatedClassLoader = classLoaderRunnerFactory.createClassLoader(klass, artifactUrlClassification);

        try
        {
            return isolatedClassLoader.loadClass(klass.getName());
        }
        catch (Exception e)
        {
            throw new InitializationError(e);
        }
    }

    @Override
    protected synchronized Statement methodBlock(FrameworkMethod method)
    {
        FrameworkMethod newMethod = null;
        try
        {
            // Need the class from the custom loader now, so lets load the class.
            Thread.currentThread().setContextClassLoader(getTestClass().getJavaClass().getClassLoader());
            // The method as parameter is from the original class and thus not found in our
            // class loaded by the custom name (reflection is class loader sensitive)
            // So find the same method but now in the class from the class Loader.
            newMethod =
                    new FrameworkMethod(
                            getTestClass().getJavaClass().getMethod(method.getName()));
        }
        catch (Exception e)
        {
            // Show any problem nicely as a JUnit Test failure.
            return new Fail(e);
        }

        // We can carry out the normal JUnit functionality with our newly discovered method now.
        return super.methodBlock(newMethod);
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier)
    {
        super.runChild(method, notifier);
        Thread.currentThread().setContextClassLoader(ClassLoaderPerTestRunner.class.getClassLoader());
    }
}
