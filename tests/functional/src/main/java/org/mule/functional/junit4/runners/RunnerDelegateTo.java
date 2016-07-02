/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.AllTests;

/**
 * Specifies the {@link Runner} that {@link ClassLoaderIsolatedTestRunner} delegates to.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RunnerDelegateTo
{

    /**
     * @return the {@link Runner} that would be used to delegate the execution of the test.
     */
    Class<? extends Runner> value() default DefaultJUnitRunner.class;

    final class DefaultJUnitRunner extends Runner {

        private final Runner wrappedDefaultRunner;

        public DefaultJUnitRunner(Class<?> testClass) throws Throwable {
            wrappedDefaultRunner = createDefaultRunner(testClass);
        }

        private static Runner createDefaultRunner(Class<?> testClass)
                throws Throwable {
            try {
                Method suiteMethod = testClass.getMethod("suite");
                if (junit.framework.Test.class.isAssignableFrom(suiteMethod.getReturnType())) {
                    return new AllTests(testClass);
                } else {
                    /* Continue below ... */
                }
            } catch (NoSuchMethodException thereIsNoSuiteMethod) {
                /* Continue below ... */
            }
            if (junit.framework.TestCase.class.isAssignableFrom(testClass)) {
                return new JUnit38ClassRunner(testClass);
            //} else if (JUnitVersion.isGreaterThanOrEqualTo("4.5")) {
            //    return SinceJUnit_4_5.createRunnerDelegate(testClass);
            } else {
                return new JUnit4ClassRunner(testClass);
            }
        }

        @Override
        public Description getDescription() {
            return wrappedDefaultRunner.getDescription();
        }

        @Override
        public void run(RunNotifier notifier) {
            wrappedDefaultRunner.run(notifier);
        }
    }

    ///**
    // * Stuff that needs to be handled in a separate class, because it
    // * deals with API that did not exist before JUnit-4.5. Having this inside
    // * {@link DefaultJUnitRunner} would cause runtime error when JUnit-4.4
    // * or earlier is used.
    // */
    //class SinceJUnit_4_5 {
    //    static Runner createRunnerDelegate(Class<?> testClass) throws InitializationError
    //    {
    //        return new JUnit4(testClass);
    //    }
    //    public static Class[] runnerAlternativeConstructorParams() {
    //        return new Class[] {Class.class, RunnerBuilder.class};
    //    }
    //    public static Object newRunnerBuilder() {
    //        return new AllDefaultPossibilitiesBuilder(false);
    //    }
    //}
}
