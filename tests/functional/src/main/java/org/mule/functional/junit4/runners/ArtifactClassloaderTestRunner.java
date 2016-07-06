/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.runners;

import static java.util.Collections.emptyList;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

/**
 * Runner that does the testing of the class using a different {@link ClassLoader} from the one that launched the test.
 *
 * @since 4.0
 */
public class ArtifactClassloaderTestRunner extends Suite
{
    private static final List<Runner> NO_RUNNERS = emptyList();
    private final ArrayList<Runner> runners = new ArrayList<>();

    /**
     * Creates a Runner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public ArtifactClassloaderTestRunner(final Class<?> klass) throws Throwable
    {
        super(createTestClassUsingClassLoader(klass), NO_RUNNERS);
        Class<? extends Runner> runnerClass = BlockJUnit4ClassRunner.class;
        RunnerDelegateTo runnerDelegateTo = klass.getAnnotation(RunnerDelegateTo.class);
        if (runnerDelegateTo != null)
        {
            runnerClass = runnerDelegateTo.value();
        }
        runners.add(runnerClass.cast(runnerClass.getConstructor(Class.class).newInstance(getTestClass().getJavaClass())));
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @Override
    public Description getDescription() {
        // Just to avoid having duplicated name in IDEA/Eclipse JUnit panel view due to this is a Suite, we always have only one delegate/child runner.
        return runners.get(0).getDescription();
    }


    protected void runChild(final Runner runner, final RunNotifier notifier) {
        withContextClassLoader(getTestClass().getJavaClass().getClassLoader(), () -> runner.run(notifier));
    }

    /**
     * @param klass
     * @return the {@link ClassLoader} that would be used to run the test. This way the test will be isolated and it will behave
     * similar as an application running in a Mule standalone container.
     * @throws Throwable
     */
    private static Class<?> createTestClassUsingClassLoader(Class<?> klass) throws Throwable
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
        ClassLoaderTestRunner classLoaderTestRunner = classLoaderRunnerFactory.createClassLoader(klass, artifactUrlClassification);

        Class<?> isolatedTestClass = classLoaderTestRunner.loadClassWithApplicationClassLoader(klass.getName());

        injectPluginsClassLoaders(classLoaderTestRunner, isolatedTestClass);

        return isolatedTestClass;
    }

    /**
     * Invokes the method to inject the plugin/extension classloaders for registering the extensions to the {@link org.mule.runtime.core.api.MuleContext}.
     * @param classLoaderTestRunner
     * @param isolatedTestClass
     * @throws Throwable
     */
    private static void injectPluginsClassLoaders(ClassLoaderTestRunner classLoaderTestRunner, Class<?> isolatedTestClass) throws Throwable
    {
        TestClass testClass = new TestClass(isolatedTestClass);
        Class<? extends Annotation> artifactContextAwareAnn = (Class<? extends Annotation>) classLoaderTestRunner.loadClassWithApplicationClassLoader(ArtifactClassloaderRunnerContextAware.class.getName());
        List<FrameworkMethod> contextAwareMethods = testClass.getAnnotatedMethods(artifactContextAwareAnn);
        for (FrameworkMethod method : contextAwareMethods)
        {
            if (!method.isStatic() || !method.isPublic())
            {
                throw new IllegalStateException("Method marked with annotation " + ArtifactClassloaderRunnerContextAware.class.getName() + " should be public static and it should receive a parameter of type List<" + ArtifactClassLoader.class + ">");
            }
            try
            {
                method.invokeExplosively(null, classLoaderTestRunner.getPlugins());
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalStateException("Method marked with annotation " + ArtifactClassloaderRunnerContextAware.class.getName() + " should receive a parameter of type List<" + ArtifactClassLoader.class + ">");
            }
        }
    }

}
