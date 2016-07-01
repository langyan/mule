/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * TODO this should be the new Artifact
 */
public class ClassLoaderPerTestRunner extends BlockJUnit4ClassRunner
{

    // The classpath is needed because the custom class loader looks there to find the classes.
    private Set<URL> classPath;
    private boolean classPathDetermined = false;

    private LinkedHashMap<MavenArtifact, Set<MavenArtifact>> dependencies;
    private boolean dependenciesResolved = false;


    // Some data related to the class from the custom class loader.
    private TestClass testClassFromClassLoader;

    private Object beforeFromClassLoader;
    private Object afterFromClassLoader;
    private Object ruleFromClassLoader;

    private Object beforeClassFromClassLoader;
    private Object afterClassFromClassLoader;
    private Object classRuleFromClassLoader;

    private ClassLoader originalCl;
    private ClassLoader runnerCl;

    private ClassPathURLsProvider classPathURLsProvider = new DefaultClassPathURLsProvider();
    private MavenDependenciesResolver mavenDependenciesResolver = new DependencyGraphMavenDependenciesResolver();
    private MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new MuleMavenMultiModuleArtifactMapping();
    private ClassLoaderRunnerFactory classLoaderRunnerFactory = new MuleClassLoaderRunnerFactory();
    private ClassPathClassifier classPathClassifier = new MuleClassPathClassifier();

    /**
     * Instantiates a new test per class loader runner.
     *
     * @param klass the class
     * @throws InitializationError the initialization error
     */
    public ClassLoaderPerTestRunner(Class<?> klass)
            throws InitializationError
    {
        super(klass);

        // Creates for the whole test methods the same class loader to be used
        ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(getTestClass().getJavaClass(), getClassPath(), getDependencies(), mavenMultiModuleArtifactMapping);
        runnerCl = classLoaderRunnerFactory.createClassLoader(getTestClass().getJavaClass(), artifactUrlClassification);
        try
        {
            testClassFromClassLoader = new TestClass(runnerCl.loadClass(getTestClass().getJavaClass().getName()));

            // See withAfters and withBefores for the reason.
            beforeFromClassLoader = runnerCl.loadClass(Before.class.getName());
            afterFromClassLoader = runnerCl.loadClass(After.class.getName());
            ruleFromClassLoader = runnerCl.loadClass(Rule.class.getName());

            beforeClassFromClassLoader = runnerCl.loadClass(BeforeClass.class.getName());
            afterClassFromClassLoader = runnerCl.loadClass(AfterClass.class.getName());
            classRuleFromClassLoader = runnerCl.loadClass(ClassRule.class.getName());
        }
        catch (Exception e)
        {
            throw new InitializationError(e);
        }

        originalCl = Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected synchronized Object createTest()
            throws Exception
    {
        // Need an instance now from the class loaded by the custom loader.
        Object instance = testClassFromClassLoader.getJavaClass().newInstance();

        List<FrameworkField> annotatedFieldsClassRule = getTestClass().getAnnotatedFields(ClassRule.class);
        for (FrameworkField each : annotatedFieldsClassRule) {
            for (FrameworkField other : testClassFromClassLoader.getAnnotatedFields((Class<? extends Annotation>) classRuleFromClassLoader))
            {
                if (other.getName().equals(each.getName()))
                {
                    try
                    {
                        other.getField().set(instance, each.get(null));
                    }
                    catch (IllegalAccessException e)
                    {
                        throw new RuntimeException(getTestClass().getName() + ": while trying to set field '" + each.getName() + "'", e);
                    }
                }
            }
        }

        return instance;
    }


    @Override
    protected synchronized Statement methodBlock(FrameworkMethod method)
    {
        FrameworkMethod newMethod = null;
        try
        {
            // Need the class from the custom loader now, so lets load the class.
            Thread.currentThread().setContextClassLoader(runnerCl);
            // The method as parameter is from the original class and thus not found in our
            // class loaded by the custom name (reflection is class loader sensitive)
            // So find the same method but now in the class from the class Loader.
            newMethod =
                    new FrameworkMethod(
                            testClassFromClassLoader.getJavaClass().getMethod(method.getName()));
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
    protected Statement withBeforeClasses(Statement statement)
    {
        List<FrameworkMethod> befores = testClassFromClassLoader
                .getAnnotatedMethods((Class<? extends Annotation>) beforeClassFromClassLoader);
        return befores.isEmpty() ? statement :
               new RunBefores(statement, befores, null);
    }

    @Override
    protected Statement withAfterClasses(Statement statement)
    {
        List<FrameworkMethod> afters = testClassFromClassLoader
                .getAnnotatedMethods((Class<? extends Annotation>) afterClassFromClassLoader);
        return afters.isEmpty() ? statement :
               new RunAfters(statement, afters, null);

    }

    protected List<TestRule> classRules() {
        List<TestRule> result = testClassFromClassLoader.getAnnotatedMethodValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class);

        result.addAll(testClassFromClassLoader.getAnnotatedFieldValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class));

        return result;
    }

    @Override
    protected synchronized Statement withAfters(FrameworkMethod method, Object target, Statement statement)
    {
        // We now to need to search in the class from the custom loader.
        //We also need to search with the annotation loaded by the custom class loader or otherwise we don't find any method.
        List<FrameworkMethod> afters =
                testClassFromClassLoader
                        .getAnnotatedMethods((Class<? extends Annotation>) afterFromClassLoader);

        return new RunAfters(statement, afters, target);
    }

    @Override
    protected synchronized Statement withBefores(FrameworkMethod method, Object target, Statement statement)
    {
        // We now to need to search in the class from the custom loader.
        //We also need to search with the annotation loaded by the custom class loader or otherwise we don't find any method.
        List<FrameworkMethod> befores =
                testClassFromClassLoader
                        .getAnnotatedMethods((Class<? extends Annotation>) beforeFromClassLoader);

        return new RunBefores(statement, befores, target);
    }

    protected List<TestRule> lateClassRules() {
        List<TestRule> result = testClassFromClassLoader.getAnnotatedMethodValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class);

        result.addAll(testClassFromClassLoader.getAnnotatedFieldValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class));

        return result;
    }


    @Override
    protected synchronized List<TestRule> getTestRules(Object target)
    {
        List<TestRule> result =
                testClassFromClassLoader
                        .getAnnotatedMethodValues(target, (Class<? extends Annotation>) ruleFromClassLoader, TestRule.class);

        result.addAll(
                testClassFromClassLoader
                        .getAnnotatedFieldValues(target, (Class<? extends Annotation>) ruleFromClassLoader, TestRule.class));

        return result;
    }

    @Override
    protected synchronized List<MethodRule> rules(Object target)
    {
        List<MethodRule> rules =
                testClassFromClassLoader
                        .getAnnotatedMethodValues(target, (Class<? extends Annotation>) ruleFromClassLoader, MethodRule.class);

        rules.addAll(
                testClassFromClassLoader
                        .getAnnotatedFieldValues(target, (Class<? extends Annotation>) ruleFromClassLoader, MethodRule.class));

        return rules;
    }

    private Set<URL> getClassPath()
    {
        if (classPathDetermined)
        {
            return classPath;
        }

        classPathDetermined = true;
        classPath = classPathURLsProvider.getURLs();
        return classPath;
    }

    private LinkedHashMap<MavenArtifact, Set<MavenArtifact>> getDependencies()
    {
        if (dependenciesResolved)
        {
            return dependencies;
        }

        dependenciesResolved = true;
        dependencies = mavenDependenciesResolver.buildDependencies();
        return dependencies;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier)
    {
        super.runChild(method, notifier);
        Thread.currentThread().setContextClassLoader(originalCl);
    }
}
