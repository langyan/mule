/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * A default implementation of {@link ClassPathClassifier}
 *
 * @since 4.0
 */
public class DefaultClassPathClassifierContext implements ClassPathClassifierContext
{

    private final Class<?> testClass;
    private final List<URL> classPathURLs;
    private final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> mavenDependencies;
    private final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping;

    public DefaultClassPathClassifierContext(final Class<?> testClass, final List<URL> classPathURLs, final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> mavenDependencies, final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping)
    {
        this.testClass = testClass;
        this.classPathURLs = classPathURLs;
        this.mavenDependencies = mavenDependencies;
        this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;
    }

    @Override
    public Class<?> getTestClass()
    {
        return testClass;
    }

    @Override
    public List<URL> getClassPathURLs()
    {
        return classPathURLs;
    }

    @Override
    public LinkedHashMap<MavenArtifact, Set<MavenArtifact>> getMavenDependencies()
    {
        return mavenDependencies;
    }

    @Override
    public MavenMultiModuleArtifactMapping getMavenMultiModuleArtifactMapping()
    {
        return mavenMultiModuleArtifactMapping;
    }
}
