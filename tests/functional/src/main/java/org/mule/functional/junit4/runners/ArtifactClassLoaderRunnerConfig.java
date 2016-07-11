/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import org.mule.runtime.extension.api.annotation.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a configuration needed by {@link ArtifactClassLoaderRunner} in order to
 * run the test.
 * <p/>
 * Be aware that this annotation will be loaded for the whole module being tested, it is not supported
 * to have different annotated values for different test classes due to in order to improve the performance for
 * building the {@link ClassLoader} it is created the first time only and used to run several tests.
 * <p/>
 * A best practice is to a base abstract class for your module tests that extends {@link org.mule.functional.junit4.ArtifactFunctionalTestCase}
 * and defines if needed anything related to the configuration with this annotation that will be applied to all the tests that
 * are being executed for the same VM.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ArtifactClassLoaderRunnerConfig
{

    /**
     * In case if a particular test needs to add extra boot packages to append to the ones already defined in the file, it will
     * have to define it here by using this annotation method.
     *
     * @return a comma separated list of packages to be added as PARENT_ONLY for the
     * container class loader, default (and required) packages is empty.
     * <p/>
     * A default list of extra boot packages is always added to the container class loader, it is defined by the excluded.properties file.
     */
    String extraBootPackages() default "";

    /**
     * The discovery process will look for classes in classpath annotated with {@link Extension}.
     *
     * @return {@link String} to define the base package to be used when discovering for extensions in order to create for each
     * extension a plugin {@link ClassLoader}. If no extension are discovered plugin class loaders will not be created.
     */
    String extensionBasePackage() default "org.mule.extension";

    /**
     * List of GAT to define the exclusions of artifacts that shouldn't be added to the application {@link ClassLoader}.
     * Default exclusion is already defined in {@code excluded.properties} file and by using this annotation the ones defined here will be appended
     * to those defined in file.
     *
     * @return a comma separated list of groupId:artifactId:type (it does support wildcards org.mule:*:* or *:mule-core:* but
     * only starts with for partial matching org.mule*:*:*) that would be used in order to exclude artifacts that should not be added to
     * the application class loader neither the extension/plugin class loaders due to they will be already exposed through the container.
     */
    String exclusions() default "";

}
