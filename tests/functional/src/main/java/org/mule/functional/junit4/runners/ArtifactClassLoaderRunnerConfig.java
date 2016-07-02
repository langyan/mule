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

/**
 * Specifies a configuration needed by {@link ArtifactClassloaderTestRunner} in order to
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
     * In case of having to append one you should also include the default list.
     */
    String extraBootPackages() default "";

    /**
     * @return array of classes defining extensions that need to be added as plugins to the classloader used
     * to execute the test. Non null;
     */
    Class[] extensions() default {};

    /**
     * @return a comma separated list of groupId:artifactId:type (it does support wildcards org.mule:*:* or *:mule-core:* but
     * only starts with for partial matching org.mule*:*:*) that would be used in order to exclude artifacts that should not be added to
     * the application class loader neither the extension/plugin class loaders due to they will be already exposed through the container.
     * Default exclusion is already defined in excluded.properties file and by using this annotation the ones defined here will be appended
     * to those defined in file.
     */
    String exclusions() default "";

}
