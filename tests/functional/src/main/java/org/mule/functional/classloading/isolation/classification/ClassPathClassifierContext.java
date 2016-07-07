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
public interface ClassPathClassifierContext
{
    /**
     * @return the {@link Class} for the test that is going to be executed
     */
    Class<?> getTestClass();

    /**
     * @return a {@link List<URL>} of URLs for the classpath provided by JUnit (it is the complete list of URLs)
     */
    List<URL> getClassPathURLs();

    /**
     * @return a {@link LinkedHashMap< MavenArtifact , Set<MavenArtifact>>} Maven dependencies for the given artifact
     * tested (with its duplications). The map has as key an artifact and values are its dependencies
     */
    LinkedHashMap<MavenArtifact, Set<MavenArtifact>> getMavenDependencies();

    /**
     * @return {@link MavenMultiModuleArtifactMapping} mapper for artifactIds and multi-module folders.
     */
    MavenMultiModuleArtifactMapping getMavenMultiModuleArtifactMapping();
}
