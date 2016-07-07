/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.classloading.isolation.maven.dependencies;

import static java.util.Collections.emptySet;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible of filtering, traversing and selectDependencies a list of URLs with different conditions and patterns in order to
 * build class loaders by filtering an initial and complete classpath urls and using a maven dependency graph represented
 * by their dependencies transitions in a {@link java.util.Map}.
 *
 * @since 4.0
 */
public class DependencyResolver
{

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    private ConfigurationBuilder configurationBuilder;

    public DependencyResolver(ConfigurationBuilder configurationBuilder)
    {
        this.configurationBuilder = configurationBuilder;
    }

    /**
     * Resolves the dependencies by applying the strategy configured in the {@link ConfigurationBuilder}
     * @return a non-null {@link Set} of {@link MavenArtifact} representing the resolved dependencies
     */
    public Set<MavenArtifact> resolveDependencies()
    {
        // Filter dependencies (first entry set in LinkedHashMap is root artifact from the dependency tree)
        MavenArtifact rootMavenArtifact = configurationBuilder.getAllDependencies().keySet().stream().findFirst().get();
        if (rootMavenArtifact == null)
        {
            return emptySet();
        }
        Set<MavenArtifact> dependencies = configurationBuilder.getAllDependencies().get(rootMavenArtifact)
                .stream().filter(key -> configurationBuilder.getDependenciesFilterBuilder().getPredicate().test(key)).collect(Collectors.toSet());

        Set<MavenArtifact> resolvedDependencies = new HashSet<>();
        if (configurationBuilder.getDependenciesFilterBuilder().isIncludeInResult())
        {
            resolvedDependencies.addAll(dependencies);
        }

        dependencies.stream().map(artifact -> getTransitiveDependencies(artifact, configurationBuilder.getTransitiveDependencyFilterBuilder().getPredicate(),
                                                                        configurationBuilder.getTransitiveDependencyFilterBuilder().isIncludeTransitiveFromFiltered()))
                .forEach(resolvedDependencies::addAll);

        if ((configurationBuilder.getRootArtifactPredicate() != null && configurationBuilder.getRootArtifactPredicate().test(rootMavenArtifact)) ||
            configurationBuilder.isRootArtifactIncludedInResults())
        {
            resolvedDependencies.add(rootMavenArtifact);
        }

        return resolvedDependencies;
    }

    /**
     * It builds a list of MavenArtifact representing the transitive dependencies of the provided dependency {@link MavenArtifact}.
     *
     * @param dependency a {@link MavenArtifact} for which we want to know its dependencies.
     * @param predicate a filter to be applied for each transitive dependency, if the filter passes the dependency is added and recursively collected its dependencies using the same filter.
     * @param shouldAddTransitiveDepFromExcluded if true does not stop when a dependency does not match the filter and collects it dependencies too.
     * @return recursively gets the dependencies for the given artifact.
     */
    private Set<MavenArtifact> getTransitiveDependencies(final MavenArtifact dependency, final Predicate<MavenArtifact> predicate, final boolean shouldAddTransitiveDepFromExcluded)
    {
        Set<MavenArtifact> transitiveDependencies = new HashSet<>();

        if (configurationBuilder.getAllDependencies().containsKey(dependency))
        {
            configurationBuilder.getAllDependencies().get(dependency).stream().forEach(transitiveDependency -> {
                if (predicate.test(transitiveDependency))
                {
                    transitiveDependencies.add(transitiveDependency);
                    transitiveDependencies.addAll(getTransitiveDependencies(transitiveDependency, predicate, shouldAddTransitiveDepFromExcluded));
                }
                else
                {
                    // Just the case for getting all their dependencies from an excluded dependencies (case of org.mule:core for instance, we also need their transitive dependencies)
                    if (shouldAddTransitiveDepFromExcluded)
                    {
                        transitiveDependencies.addAll(getTransitiveDependencies(transitiveDependency, predicate, shouldAddTransitiveDepFromExcluded));
                    }
                }
            });
        }
        return transitiveDependencies;
    }

}