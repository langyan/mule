/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.runners;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;
import org.mule.functional.classloading.isolation.maven.dependencies.ConfigurationBuilder;
import org.mule.functional.classloading.isolation.maven.dependencies.DependenciesFilterBuilder;
import org.mule.functional.classloading.isolation.maven.dependencies.DependencyResolver;
import org.mule.functional.classloading.isolation.maven.dependencies.TransitiveDependenciesFilterBuilder;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.tck.size.SmallTest;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link DependencyResolver}
 */
@SmallTest
public class DependencyResolverTest
{

    private DependencyResolver builder;

    private MavenArtifact rootArtifact;
    private MavenArtifact commonsLangArtifact;
    private MavenArtifact gsonArtifact;
    private MavenArtifact commonsCliArtifact;
    private MavenArtifact dom4JArtifact;
    private MavenArtifact javaxInjectArtifact;
    private MavenArtifact junitArtifact;

    @Before
    public void setUp() throws MalformedURLException
    {
        buildDefaultArtifacts();
    }

    @Test
    public void excludeRootSelectProvidedDependenciesOnlyTestTransitiveDependencies()
    {
        builder = new DependencyResolver(new ConfigurationBuilder()
                                             .setMavenDependencyGraph(buildDefaultDependencies())
                                             .selectDependencies(
                                                     new DependenciesFilterBuilder()
                                                             .match(dependency -> dependency.isProvidedScope())
                                                             .doNotIncludeInResult()
                                             )
                                             .collectTransitiveDependencies(
                                                     new TransitiveDependenciesFilterBuilder()
                                                             .match(dependency -> dependency.isTestScope())
                                                             .includeTransitiveDependenciesFromFiltered()
                                             )
        );

        assertTrue(builder.resolveDependencies().isEmpty());
    }

    @Test
    public void collectDependenciesUsingDependencyAsRootArtifact()
    {
        ValueHolder<MavenArtifact> selectedRootArtifactHolder = new ValueHolder<>(rootArtifact);

        builder = new DependencyResolver(new ConfigurationBuilder()
                                                 .setMavenDependencyGraph(buildDefaultDependencies())
                                                 .includeRootArtifactInResults(artifact -> artifact.getArtifactId().equals(selectedRootArtifactHolder.get().getArtifactId()))
                                                 .selectDependencies(
                                                         new DependenciesFilterBuilder()
                                                                 .match(dependency -> dependency.getArtifactId().equals(selectedRootArtifactHolder.get().getArtifactId())
                                                                                      || (rootArtifact.getArtifactId().equals(selectedRootArtifactHolder.get().getArtifactId()) && dependency.isProvidedScope())))
                                                 .collectTransitiveDependencies(
                                                         new TransitiveDependenciesFilterBuilder()
                                                                 .match(transitiveDependency -> transitiveDependency.isProvidedScope())
                                                 )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(3));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(rootArtifact));
        assertThat(results.get(2), equalTo(dom4JArtifact));

        // Now we change the selectedRootArtifact to commonsCli
        selectedRootArtifactHolder.set(commonsCliArtifact);

        dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(dom4JArtifact));
    }

    @Test
    public void onlyTestTransitiveDependencies()
    {
        builder = new DependencyResolver(new ConfigurationBuilder()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .selectDependencies(
                                                             new DependenciesFilterBuilder()
                                                                     .doNotIncludeInResult()
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isTestScope())
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(1));
        MavenArtifact mavenArtifact = dependencies.iterator().next();
        assertThat(mavenArtifact, equalTo(junitArtifact));
    }

    @Test
    public void excludeRootOnlyProvidedDependencies()
    {
        builder = new DependencyResolver(new ConfigurationBuilder()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .selectDependencies(
                                                             new DependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isProvidedScope())
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isProvidedScope())
                                                                     .includeTransitiveDependenciesFromFiltered()
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(dom4JArtifact));
    }

    @Test
    public void onlyProvidedDependenciesIncludingRootArtifactWithoutTransitiveDependencies()
    {
        builder = new DependencyResolver(new ConfigurationBuilder()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .includeRootArtifactInResults()
                                                     .selectDependencies(
                                                             new DependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isProvidedScope())
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsCliArtifact));
        assertThat(results.get(1), equalTo(rootArtifact));
    }

    @Test
    public void excludeRootOnlyCompileDependencies()
    {
        builder = new DependencyResolver(new ConfigurationBuilder()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .selectDependencies(
                                                             new DependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                                     .includeTransitiveDependenciesFromFiltered()
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(2));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsLangArtifact));
        assertThat(results.get(1), equalTo(gsonArtifact));
    }

    @Test
    public void onlyCompileDependenciesIncludingRootArtifact()
    {
        builder = new DependencyResolver(new ConfigurationBuilder()
                                                     .setMavenDependencyGraph(buildDefaultDependencies())
                                                     .includeRootArtifactInResults()
                                                     .selectDependencies(
                                                             new DependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                                     .includeTransitiveDependenciesFromFiltered()
                                                     )
        );

        Set<MavenArtifact> dependencies = builder.resolveDependencies();

        assertThat(dependencies.size(), equalTo(3));
        List<MavenArtifact> results = sortArtifacts(dependencies);
        assertThat(results.get(0), equalTo(commonsLangArtifact));
        assertThat(results.get(1), equalTo(rootArtifact));
        assertThat(results.get(2), equalTo(gsonArtifact));
    }

    @Test
    public void excludeRootOnlyProvidedAndTransitiveDependencies()
    {
        LinkedHashMap<MavenArtifact, Set<MavenArtifact>> dependencies = new LinkedHashMap<>();

        dom4JArtifact = buildMavenArtifact(dom4JArtifact.getGroupId(), dom4JArtifact.getArtifactId(), dom4JArtifact.getType(), dom4JArtifact.getVersion(), "compile");

        // Dependencies
        Set<MavenArtifact> rootDependencies = new HashSet<>();
        rootDependencies.add(commonsCliArtifact);

        Set<MavenArtifact> commonsCliDependencies = new HashSet<>();
        commonsCliDependencies.add(dom4JArtifact);

        dependencies.put(rootArtifact, rootDependencies);
        dependencies.put(commonsCliArtifact, commonsCliDependencies);

        builder = new DependencyResolver(new ConfigurationBuilder()
                                                     .setMavenDependencyGraph(dependencies)
                                                     .selectDependencies(
                                                             new DependenciesFilterBuilder()
                                                                     .doNotIncludeInResult()
                                                     )
                                                     .collectTransitiveDependencies(
                                                             new TransitiveDependenciesFilterBuilder()
                                                                     .match(dependency -> dependency.isCompileScope())
                                                                     .includeTransitiveDependenciesFromFiltered()
                                                     )
        );

        Set<MavenArtifact> results = builder.resolveDependencies();

        assertThat(results.size(), equalTo(1));
        assertThat(results.iterator().next(), equalTo(dom4JArtifact));
    }

    private List<MavenArtifact> sortArtifacts(Set<MavenArtifact> mavenArtifacts)
    {
        return mavenArtifacts.stream().sorted((a1, a2) -> a1.getArtifactId().compareTo(a2.getArtifactId())).collect(Collectors.toList());
    }

    private MavenArtifact buildMavenArtifact(String groupId, String artifactId, String type, String version, String scope)
    {
        return new MavenArtifact(groupId, artifactId, type, version, scope);
    }

    private void buildDefaultArtifacts()
    {
        rootArtifact = buildMavenArtifact("org.my.company", "core-artifact", "jar", "1.0.0", "compile");
        commonsLangArtifact = buildMavenArtifact("org.apache.commons", "commons-lang3", "jar", "3.4", "compile");
        gsonArtifact = buildMavenArtifact("com.google.code.gson", "gson", "jar", "2.6.2", "compile");
        commonsCliArtifact = buildMavenArtifact("commons-cli", "commons-cli", "jar", "1.2", "provided");
        dom4JArtifact = buildMavenArtifact("dom4j", "dom4j", "jar", "1.6.1", "provided");
        javaxInjectArtifact = buildMavenArtifact("javax.inject", "javax.inject", "jar", "1.0", "provided");
        junitArtifact = buildMavenArtifact("junit", "junit", "jar", "4.12", "test");
    }

    private LinkedHashMap<MavenArtifact, Set<MavenArtifact>> buildDefaultDependencies()
    {
        LinkedHashMap<MavenArtifact, Set<MavenArtifact>> dependencies = new LinkedHashMap<>();

        // Dependencies
        Set<MavenArtifact> rootDependencies = new HashSet<>();
        rootDependencies.add(commonsLangArtifact);
        rootDependencies.add(gsonArtifact);
        rootDependencies.add(commonsCliArtifact);

        Set<MavenArtifact> commonsCliDependencies = new HashSet<>();
        commonsCliDependencies.add(dom4JArtifact);

        Set<MavenArtifact> gsonDependencies = new HashSet<>();
        gsonDependencies.add(javaxInjectArtifact);

        Set<MavenArtifact> commonsLangDependencies = new HashSet<>();
        commonsLangDependencies.add(junitArtifact);

        dependencies.put(rootArtifact, rootDependencies);
        dependencies.put(commonsCliArtifact, commonsCliDependencies);
        dependencies.put(gsonArtifact, gsonDependencies);
        dependencies.put(commonsLangArtifact, commonsLangDependencies);

        return dependencies;
    }
} 