/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test;

import org.mule.functional.junit4.ArtifactFunctionalTestCase;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;

@ArtifactClassLoaderRunnerConfig(extensions = "org.mule.extension.validation.api.ValidationExtension",
extraBootPackages =
        //TODO(gfernandes): need to expose every package form groovy
        "org.codehaus.groovy," +
        //TODO(gfernandes): review why this is required as it is exported on scripting mule-module.properties (fails ClassInterceptorTestCase)
        "org.aopalliance.aop")
public abstract class AbstractIntegrationTestCase extends ArtifactFunctionalTestCase
{

}
