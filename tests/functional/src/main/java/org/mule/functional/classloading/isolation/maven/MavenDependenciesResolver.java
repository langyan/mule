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
     * Creates a dependency graph where the key represents a {@link MavenArtifact} and value defines its dependencies.
     * For each dependency if there is a transtive dependency a key would be present in {@link LinkedHashMap<MavenArtifact, Set<MavenArtifact>>}.
     * The first key in the {@link LinkedHashMap<MavenArtifact, Set<MavenArtifact>>}.
     *
     * @return it generates the dependencies for the maven artifact where the resolver is being called.
     * It returns a {@link LinkedHashMap} with each dependency as key and for each key a {@link Set} of its dependencies.
     * First entry of the map should be the current artifact being tested by the runner.
     */
    LinkedHashMap<MavenArtifact, Set<MavenArtifact>> buildDependencies();
}
