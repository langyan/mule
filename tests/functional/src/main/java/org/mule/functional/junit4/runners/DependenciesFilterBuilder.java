/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import org.mule.functional.util.TruePredicate;

import java.util.function.Predicate;

/**
 * Filter definition for selecting dependencies when resolving them from {@link DependencyResolver}
 */
public final class DependenciesFilterBuilder
{
    private Predicate<MavenArtifact> predicate = new TruePredicate<>();
    private boolean includeInResult = true;

    public DependenciesFilterBuilder match(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
        return this;
    }

    public DependenciesFilterBuilder doNotIncludeInResult()
    {
        this.includeInResult = false;
        return this;
    }

    Predicate<MavenArtifact> getPredicate()
    {
        return predicate;
    }

    boolean isIncludeInResult()
    {
        return includeInResult;
    }

}
