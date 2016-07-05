/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import org.mule.functional.util.TruePredicate;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builder for setting the resolution strategy for {@link DependencyResolver}
 */
public final class ConfigurationBuilder
{

    private LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies;
    private boolean rootArtifactIncludedInResults = false;
    private DependenciesFilterBuilder dependenciesFilterBuilder;
    private TransitiveDependenciesFilterBuilder transitiveDependencyFilterBuilder = new TransitiveDependenciesFilterBuilder(new TruePredicate<MavenArtifact>().negate());
    private Predicate<MavenArtifact> rootArtifactPredicate = null;

    public ConfigurationBuilder setMavenDependencyGraph(LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies)
    {
        this.allDependencies = allDependencies;
        return this;
    }

    public ConfigurationBuilder includeRootArtifactInResults()
    {
        this.rootArtifactIncludedInResults = true;
        return this;
    }

    public ConfigurationBuilder includeRootArtifactInResults(Predicate<MavenArtifact> rootArtifactPredicate)
    {
        this.rootArtifactPredicate = rootArtifactPredicate;
        return this;
    }

    public ConfigurationBuilder selectDependencies(DependenciesFilterBuilder dependenciesFilterBuilder)
    {
        this.dependenciesFilterBuilder = dependenciesFilterBuilder;
        return this;
    }

    public ConfigurationBuilder collectTransitiveDependencies(TransitiveDependenciesFilterBuilder transitiveDependenciesFilterBuilder)
    {
        this.transitiveDependencyFilterBuilder = transitiveDependenciesFilterBuilder;
        return this;
    }

    LinkedHashMap<MavenArtifact, Set<MavenArtifact>> getAllDependencies()
    {
        return allDependencies;
    }

    DependenciesFilterBuilder getDependenciesFilterBuilder()
    {
        return dependenciesFilterBuilder;
    }

    TransitiveDependenciesFilterBuilder getTransitiveDependencyFilterBuilder()
    {
        return transitiveDependencyFilterBuilder;
    }

    boolean isRootArtifactIncludedInResults()
    {
        return this.rootArtifactIncludedInResults;
    }

    Predicate<MavenArtifact> getRootArtifactPredicate()
    {
        return rootArtifactPredicate;
    }
}