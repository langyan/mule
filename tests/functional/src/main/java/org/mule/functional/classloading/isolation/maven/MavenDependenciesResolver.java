/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Resolves maven dependencies for the artifact being tested.
 *
 * @since 4.0
 */
public interface MavenDependenciesResolver
{

    /**
     * @return it would generate the dependencies for the maven artifact where the resolver is being called.
     * It will return a {@link LinkedHashMap} with each dependency as key and for each key a {@link Set} of its dependencies.
     * First entry of the map should be the current artifact being tested by the runner.
     */
    LinkedHashMap<MavenArtifact, Set<MavenArtifact>> buildDependencies();
}
