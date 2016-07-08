/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven.dependencies;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.util.TruePredicate;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Builder for setting the resolution strategy for {@link DependencyResolver}
 *
 * @since 4.0
 */
public final class ConfigurationBuilder
{

    private LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies;
    private boolean rootArtifactIncluded = false;
    private DependenciesFilterBuilder dependenciesFilterBuilder = new DependenciesFilterBuilder();
    private TransitiveDependenciesFilterBuilder transitiveDependencyFilterBuilder = new TransitiveDependenciesFilterBuilder(new TruePredicate<MavenArtifact>().negate());
    private Predicate<MavenArtifact> rootArtifactPredicate = null;

    /**
     * Sets the dependency tree, it is structure as links in a {@link LinkedHashMap<MavenArtifact>, Set<MavenArtifact>>} where
     * the first entry has as key the root of the tree and as value the list of dependencies, for each one of those (if there is the case)
     * there will be another entry with value as their dependencies so the whole tree is represented in this way.
     *
     * @param allDependencies
     * @return this
     */
    public ConfigurationBuilder setMavenDependencyGraph(LinkedHashMap<MavenArtifact, Set<MavenArtifact>> allDependencies)
    {
        this.allDependencies = allDependencies;
        return this;
    }

    /**
     * It sets the strategy to also include the root artifact in the result of the dependencies resolved.
     * By default it is not included due to the most common usage is to get dependencies instead of the whole
     * set of root artifact plus dependencies.
     *
     * @return this
     */
    public ConfigurationBuilder includeRootArtifact()
    {
        this.rootArtifactIncluded = true;
        return this;
    }

    /**
     * A conditional way to define if the root artifact should be included or not in results. A {@link Predicate<MavenArtifact>}
     * can be passed that will be evaluated with the root artifact during the resolution of the dependencies.
     *
     * @param rootArtifactPredicate
     * @return this
     */
    public ConfigurationBuilder includeRootArtifact(Predicate<MavenArtifact> rootArtifactPredicate)
    {
        this.rootArtifactPredicate = rootArtifactPredicate;
        return this;
    }

    /**
     * Sets a {@link DependenciesFilterBuilder} that defines the strategy for selecting the dependencies.
     * If this is not defined, by default, the resolver will take all the dependencies of the root artifact
     * without their transitive dependencies.
     *
     * @param dependenciesFilterBuilder
     * @return this
     */
    public ConfigurationBuilder selectDependencies(DependenciesFilterBuilder dependenciesFilterBuilder)
    {
        this.dependenciesFilterBuilder = dependenciesFilterBuilder;
        return this;
    }

    /**
     * Sets a {@link TransitiveDependenciesFilterBuilder} that defines the strategy for selecting the transitive
     * dependencies for the dependencies of the root artifact that matched the criteria defined.
     * By default (if this is not set) no transitive dependencies will be collected during the dependencies resolution.
     *
     * @param transitiveDependenciesFilterBuilder
     * @return this
     */
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

    boolean isRootArtifactIncluded()
    {
        return this.rootArtifactIncluded;
    }

    Predicate<MavenArtifact> getIncludeRootArtifactPredicate()
    {
        return rootArtifactPredicate;
    }
}