/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.lang.annotation.Annotation;
import java.util.List;

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
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * TODO this should be the new Artifact, add support for delegate
 */
public class ClassLoaderPerTestRunner extends BlockJUnit4ClassRunner
{
    private Object beforeFromClassLoader;
    private Object afterFromClassLoader;
    private Object ruleFromClassLoader;

    private static Object beforeClassFromClassLoader;
    private static Object afterClassFromClassLoader;
    private static Object classRuleFromClassLoader;

    private ClassLoader originalCl;
    private static ClassLoader runnerCl;

    private static ClassPathURLsProvider classPathURLsProvider = new DefaultClassPathURLsProvider();
    private static MavenDependenciesResolver mavenDependenciesResolver = new DependencyGraphMavenDependenciesResolver();
    private static MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping = new MuleMavenMultiModuleArtifactMapping();
    private static ClassLoaderRunnerFactory classLoaderRunnerFactory = new MuleClassLoaderRunnerFactory();
    private static ClassPathClassifier classPathClassifier = new MuleClassPathClassifier();

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

        originalCl = Thread.currentThread().getContextClassLoader();
    }

    private static Class<?> createTestClassUsingClassLoader(Class<?> klass) throws InitializationError
    {
        if (runnerCl == null)
        {
            // Creates for the whole test methods the same class loader to be used
            ArtifactUrlClassification artifactUrlClassification = classPathClassifier.classify(klass, classPathURLsProvider.getURLs(),
                                                                                               mavenDependenciesResolver.buildDependencies(), mavenMultiModuleArtifactMapping);
            runnerCl = classLoaderRunnerFactory.createClassLoader(klass, artifactUrlClassification);

            try
            {
                beforeClassFromClassLoader = runnerCl.loadClass(BeforeClass.class.getName());
                afterClassFromClassLoader = runnerCl.loadClass(AfterClass.class.getName());
                classRuleFromClassLoader = runnerCl.loadClass(ClassRule.class.getName());
            }
            catch (Exception e)
            {
                throw new InitializationError(e);
            }
        }
        try
        {
            return runnerCl.loadClass(klass.getName());
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
            // See withAfters and withBefores for the reason.
            beforeFromClassLoader = runnerCl.loadClass(Before.class.getName());
            afterFromClassLoader = runnerCl.loadClass(After.class.getName());
            ruleFromClassLoader = runnerCl.loadClass(Rule.class.getName());

            // Need the class from the custom loader now, so lets load the class.
            Thread.currentThread().setContextClassLoader(runnerCl);
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
    protected Statement withBeforeClasses(Statement statement)
    {
        List<FrameworkMethod> befores = getTestClass()
                .getAnnotatedMethods((Class<? extends Annotation>) beforeClassFromClassLoader);
        return befores.isEmpty() ? statement :
               new RunBefores(statement, befores, null);
    }

    @Override
    protected Statement withAfterClasses(Statement statement)
    {
        List<FrameworkMethod> afters = getTestClass()
                .getAnnotatedMethods((Class<? extends Annotation>) afterClassFromClassLoader);
        return afters.isEmpty() ? statement :
               new RunAfters(statement, afters, null);

    }

    protected List<TestRule> classRules() {
        List<TestRule> result = getTestClass().getAnnotatedMethodValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class);

        result.addAll(getTestClass().getAnnotatedFieldValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class));

        return result;
    }

    @Override
    protected synchronized Statement withAfters(FrameworkMethod method, Object target, Statement statement)
    {
        // We now to need to search in the class from the custom loader.
        //We also need to search with the annotation loaded by the custom class loader or otherwise we don't find any method.
        List<FrameworkMethod> afters =
                getTestClass()
                        .getAnnotatedMethods((Class<? extends Annotation>) afterFromClassLoader);

        return new RunAfters(statement, afters, target);
    }

    @Override
    protected synchronized Statement withBefores(FrameworkMethod method, Object target, Statement statement)
    {
        // We now to need to search in the class from the custom loader.
        //We also need to search with the annotation loaded by the custom class loader or otherwise we don't find any method.
        List<FrameworkMethod> befores =
                getTestClass()
                        .getAnnotatedMethods((Class<? extends Annotation>) beforeFromClassLoader);

        return new RunBefores(statement, befores, target);
    }

    protected List<TestRule> lateClassRules() {
        List<TestRule> result = getTestClass().getAnnotatedMethodValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class);

        result.addAll(getTestClass().getAnnotatedFieldValues(null, (Class<? extends Annotation>) classRuleFromClassLoader, TestRule.class));

        return result;
    }


    @Override
    protected synchronized List<TestRule> getTestRules(Object target)
    {
        List<TestRule> result =
                getTestClass()
                        .getAnnotatedMethodValues(target, (Class<? extends Annotation>) ruleFromClassLoader, TestRule.class);

        result.addAll(
                getTestClass()
                        .getAnnotatedFieldValues(target, (Class<? extends Annotation>) ruleFromClassLoader, TestRule.class));

        return result;
    }

    @Override
    protected synchronized List<MethodRule> rules(Object target)
    {
        List<MethodRule> rules =
                getTestClass()
                        .getAnnotatedMethodValues(target, (Class<? extends Annotation>) ruleFromClassLoader, MethodRule.class);

        rules.addAll(
                getTestClass()
                        .getAnnotatedFieldValues(target, (Class<? extends Annotation>) ruleFromClassLoader, MethodRule.class));

        return rules;
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier)
    {
        super.runChild(method, notifier);
        Thread.currentThread().setContextClassLoader(originalCl);
    }
}
