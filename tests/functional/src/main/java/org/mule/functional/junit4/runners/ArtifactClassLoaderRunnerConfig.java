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
 * Specifies a configuration needed by {@link ArtifactClassLoaderTestRunner} in order to
 * run the test.
 *
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ArtifactClassLoaderRunnerConfig
{

    /**
     * @return a comma separated list of packages to be added as PARENT_ONLY for the
     * container class loader, default (and required) packages is empty.
     * <p/>
     * A default list of extra boot packages is always added to the container class loader, it is defined by the excluded.properties file.
     * <p/>
     * In case if a particular test needs to add extra boot packages to append to the ones already defined in the file, it will
     * have to define it here by using this annotation method.
     */
    String extraBootPackages() default "";

    /**
     * @return {@link String} to define the base package to be used when discovering for extensions in order to create for each
     * extension a plugin {@link ClassLoader}. If no extension are discovered plugin class loaders will not be created.
     * The discovery process will look for classes in classpath annotated with {@link Extension}.
     */
    String extensionBasePackage() default "org.mule.extension";

    /**
     * @return a comma separated list of groupId:artifactId:type (it does support wildcards org.mule:*:* or *:mule-core:* but
     * only starts with for partial matching org.mule*:*:*) that would be used in order to exclude artifacts that should not be added to
     * the application class loader neither the extension/plugin class loaders due to they will be already exposed through the container.
     * <p/>
     * Default exclusion is already defined in excluded.properties file and by using this annotation the ones defined here will be appended
     * to those defined in file.
     */
    String exclusions() default "";

}
