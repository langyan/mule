/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.classloading.isolation.maven.MavenMultiModuleArtifactMapping;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Represents a context that contains what is needed in order to do a classpath classification.
 * It is used in {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ClassPathClassifierContext
{
    private final Class<?> testClass;
    private final List<URL> classPathURLs;
    private final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> mavenDependencies;
    private final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping;

    /**
     * Creates a context used for doing the classification of the class path.
     *
     * @param testClass the test {@link Class} being tested
     * @param classPathURLs the whole set of {@link URL}s that were loaded by IDE/Maven Surefire plugin when running the test
     * @param mavenDependencies the tree of maven dependencies for the artifact that the test belongs to
     * @param mavenMultiModuleArtifactMapping a mapper to get multi-module folder for artifactIds
     */
    public ClassPathClassifierContext(final Class<?> testClass, final List<URL> classPathURLs, final LinkedHashMap<MavenArtifact, Set<MavenArtifact>> mavenDependencies, final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping)
    {
        this.testClass = testClass;
        this.classPathURLs = classPathURLs;
        this.mavenDependencies = mavenDependencies;
        this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;
    }

    /**
     * @return the {@link Class} for the test that is going to be executed
     */
    public Class<?> getTestClass()
    {
        return testClass;
    }

    /**
     * @return a {@link List<URL>} of URLs for the classpath provided by JUnit (it is the complete list of URLs)
     */
    public List<URL> getClassPathURLs()
    {
        return classPathURLs;
    }

    /**
     * @return a {@link LinkedHashMap< MavenArtifact , Set<MavenArtifact>>} Maven dependencies for the given artifact
     * tested (with its duplications). The map has as key an artifact and values are its dependencies. The first key in
     * the map represents the root of the dependencies tree.
     */
    public LinkedHashMap<MavenArtifact, Set<MavenArtifact>> getMavenDependencies()
    {
        return mavenDependencies;
    }

    /**
     * @return {@link MavenMultiModuleArtifactMapping} mapper for artifactIds and multi-module folders.
     */
    public MavenMultiModuleArtifactMapping getMavenMultiModuleArtifactMapping()
    {
        return mavenMultiModuleArtifactMapping;
    }

}
