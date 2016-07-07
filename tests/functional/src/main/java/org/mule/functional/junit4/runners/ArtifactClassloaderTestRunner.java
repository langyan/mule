/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.runners;

import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.TestClass;

/**
 * Runner that mimics the class loading model used in a standalone container.
 * In order to detect early issues related to isolation when building plugins these runner allow you to
 * run your functional test cases using an isolated class loader.
 *
 * {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} should be extended in order to use this
 * runner, it has already annotated the runner and also has the logic to configure extension into {@link org.mule.runtime.core.api.MuleContext}.
 *
 * See {@link RunnerDelegateTo} for those scenarios where another JUnit runner needs to be used but still the test
 * has to be executed within an isolated class loading model.
 * {@link ArtifactClassLoaderRunnerConfig} allows to define the Extensions to be discovered in the classpath, for each
 * Extension a plugin class loader would be created.
 * {@link PluginClassLoadersAware} allows the test to be injected with the list of {@link ClassLoader}s that were created
 * for each plugin, mostly used in {@link org.mule.functional.junit4.ArtifactFunctionalTestCase} in order to register the extensions.
 *
 * The class loading model is built by doing a classification of the ClassPath URLs loaded by IDEs and surfire-maven-plugin.
 * The classification bases its logic by reading the dependency tree graph generated with depgraph-maven-plugin.
 * It goes over the tree to select the dependencies and getting the URLs from the Launcher class loader to create the
 * {@link ArtifactClassLoader}s and filters for each one of them.
 * See {@link ClassPathClassifier} for details about the classification logic. Just for understanding the simple way to
 * describe the classification is by saying that all the provided dependencies (including its transitives) will go to the container
 * class loader, for each extension defined it will create a plugin class loader including its compile dependencies (including transitives)
 * and the rest of the test dependencies (including transitives) will go to the application class loader.
 *
 * @since 4.0
 */
public class ArtifactClassloaderTestRunner extends Runner implements Filterable
{
    private final Runner delegate;
    private final ClassLoaderTestRunner classLoaderTestRunner;

    /**
     * Creates a Runner to run {@code klass}
     *
     * @param clazz
     * @param builder
     * @throws Throwable if there was an error while initializing the runner.
     */
    public ArtifactClassloaderTestRunner(Class<?> clazz, RunnerBuilder builder) throws Throwable
    {
        classLoaderTestRunner = createClassLoaderTestRunner(clazz);

        final Class<?> isolatedTestClass = getTestClass(clazz);

        final RunnerDelegateTo runnerDelegateToAnnotation = isolatedTestClass.getAnnotation(RunnerDelegateTo.class);
        if (runnerDelegateToAnnotation != null)
        {
            final AnnotatedBuilder annotatedBuilder = new AnnotatedBuilder(builder);
            delegate = annotatedBuilder.buildRunner(runnerDelegateToAnnotation.value(), isolatedTestClass);
        }
        else
        {
            delegate = new BlockJUnit4ClassRunner(isolatedTestClass);
        }

        injectPluginsClassLoaders(classLoaderTestRunner, isolatedTestClass);
    }

    private Class<?> getTestClass(Class<?> clazz) throws InitializationError
    {
        try
        {
            return classLoaderTestRunner.loadClassWithApplicationClassLoader(clazz.getName());
        }
        catch (Exception e)
        {
            throw new InitializationError(e);
        }
    }

    /**
     * @param klass
     * @return creates a {@link ClassLoaderTestRunner} that would be used to run the test. This way the test will be isolated and it will behave
     * similar as an application running in a Mule standalone container.
     */
    private static ClassLoaderTestRunner createClassLoaderTestRunner(Class<?> klass)
    {
        // Initializes utility classes
        ClassPathURLsProvider classPathURLsProvider = new DefaultClassPathURLsProvider();
        MavenDependenciesResolver mavenDependenciesResolver = new DependencyGraphMavenDependenciesResolver();
        MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new MuleMavenMultiModuleArtifactMapping();
        ClassLoaderRunnerFactory classLoaderRunnerFactory = new MuleClassLoaderRunnerFactory();
        ClassPathClassifier classPathClassifier = new MuleClassPathClassifier();

        // Does the classification and creation of the isolated ClassLoader
        ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(new DefaultClassPathClassifierContext(klass, classPathURLsProvider.getURLs(),
                                                                                           mavenDependenciesResolver.buildDependencies(), mavenMultiModuleArtifactMapping));
        return classLoaderRunnerFactory.createClassLoader(klass, artifactUrlClassification);
    }

    /**
     * Invokes the method to inject the plugin/extension classloaders for registering the extensions to the {@link org.mule.runtime.core.api.MuleContext}.
     *
     * @param classLoaderTestRunner
     * @param isolatedTestClass
     * @throws Throwable
     */
    private static void injectPluginsClassLoaders(ClassLoaderTestRunner classLoaderTestRunner, Class<?> isolatedTestClass) throws Throwable
    {
        TestClass testClass = new TestClass(isolatedTestClass);
        Class<? extends Annotation> artifactContextAwareAnn = (Class<? extends Annotation>) classLoaderTestRunner.loadClassWithApplicationClassLoader(PluginClassLoadersAware.class.getName());
        List<FrameworkMethod> contextAwareMethods = testClass.getAnnotatedMethods(artifactContextAwareAnn);
        for (FrameworkMethod method : contextAwareMethods)
        {
            if (!method.isStatic() || !method.isPublic())
            {
                throw new IllegalStateException("Method marked with annotation " + PluginClassLoadersAware.class.getName() + " should be public static and it should receive a parameter of type List<" + ArtifactClassLoader.class + ">");
            }
            try
            {
                method.invokeExplosively(null, classLoaderTestRunner.getPluginClassLoaders());
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalStateException("Method marked with annotation " + PluginClassLoadersAware.class.getName() + " should receive a parameter of type List<" + ArtifactClassLoader.class + ">");
            }
        }
    }

    /**
     * @return delegates to the internal runner to get the description needed by JUnit.
     */
    @Override
    public Description getDescription()
    {
        return delegate.getDescription();
    }

    /**
     * When the test is about to be executed the ThreadContextClassLoader is changed to use the application class loader that
     * was created so the execution of the test will be done using an isolated class loader that mimics the standalone container.
     *
     * @param notifier
     */
    @Override
    public void run(RunNotifier notifier)
    {
        withContextClassLoader(classLoaderTestRunner.getApplicationClassLoader().getClassLoader(), () -> delegate.run(notifier));
    }

    /**
     * Delegates to the inner runner to filter.
     *
     * @param filter
     * @throws NoTestsRemainException
     */
    @Override
    public void filter(Filter filter) throws NoTestsRemainException
    {
        if (delegate instanceof Filterable)
        {
            ((Filterable) delegate).filter(filter);
        }
    }
}
