/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven.dependencies;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.util.TruePredicate;

import java.util.function.Predicate;

/**
 * A definition for the filtering strategy to be used in {@link DependencyResolver}
 *
 * @since 4.0
 */
public final class TransitiveDependenciesFilter
{
    private Predicate<MavenArtifact> predicate = new TruePredicate<>();
    private boolean traverseWhenNoMatch = false;

    /**
     * Creates a new instance, only visible to package scope so the {@link Configuration} is the only one
     * that should call this constructor.
     *
     * @param predicate
     */
    TransitiveDependenciesFilter(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
    }

    /**
     * Public constructor, accessible by clients of this API.
     */
    public TransitiveDependenciesFilter()
    {
    }

    /**
     * {@link Predicate<MavenArtifact>} to be used to filter which transitive dependencies should be included.
     *
     * @param predicate
     * @return this
     */
    public TransitiveDependenciesFilter match(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
        return this;

    }

    /**
     * Defines that if a transitive dependency does not match the predicate, it should not be included but
     * its dependencies should be considered to be evaluated by the predicate in order to know if they should be included
     * or not. In other words, it means that the dependency tree should continue with the next child dependencies of the
     * current being evaluated instead of stop at this point. By default, if the transitive dependency does not match
     * their dependencies will not be evaluated, therefore they process will not go deeper on this path.
     *
     * @return this
     */
    public TransitiveDependenciesFilter traverseWhenNoMatch()
    {
        this.traverseWhenNoMatch = true;
        return this;
    }

    Predicate<MavenArtifact> getPredicate()
    {
        return predicate;
    }

    boolean isTraverseWhenNoMatch()
    {
        return this.traverseWhenNoMatch;
    }

}
