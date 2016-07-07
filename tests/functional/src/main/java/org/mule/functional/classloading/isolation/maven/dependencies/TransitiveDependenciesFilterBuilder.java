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
public final class TransitiveDependenciesFilterBuilder
{
    private Predicate<MavenArtifact> predicate = new TruePredicate<>();
    private boolean includeTransitiveFromFiltered = false;

    TransitiveDependenciesFilterBuilder(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
    }

    public TransitiveDependenciesFilterBuilder()
    {
    }

    public TransitiveDependenciesFilterBuilder match(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
        return this;

    }

    public TransitiveDependenciesFilterBuilder includeTransitiveDependenciesFromFiltered()
    {
        this.includeTransitiveFromFiltered = true;
        return this;
    }

    Predicate<MavenArtifact> getPredicate()
    {
        return predicate;
    }

    boolean isIncludeTransitiveFromFiltered()
    {
        return this.includeTransitiveFromFiltered;
    }

}
