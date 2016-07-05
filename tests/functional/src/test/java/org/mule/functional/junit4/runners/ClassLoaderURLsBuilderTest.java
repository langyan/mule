/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.runners;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Sets;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link ClassLoaderURLsBuilder}
 */
public class ClassLoaderURLsBuilderTest
{

    private Set<URL> urls;
    private MavenMultiModuleArtifactMapping mavenMultiModuleMapping;

    private ClassLoaderURLsBuilder builder;

    private MavenArtifact rootArtifact;
    private MavenArtifact commonsLangArtifact;
    private MavenArtifact gsonArtifact;
    private MavenArtifact commonsCliArtifact;
    private MavenArtifact dom4JArtifact;

    @Before
    public void setUp() throws MalformedURLException
    {
        buildDefaultURLs();
        buildDefaultArtifacts();
        mavenMultiModuleMapping = mock(MavenMultiModuleArtifactMapping.class);

        builder = new ClassLoaderURLsBuilder(urls, mavenMultiModuleMapping, buildDefaultDependencies());
    }

    @Test
    public void excludeRootOnlyTestTransitiveDependencies()
    {
        boolean shouldAddOnlyDependencies = true;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isTestScope());

        assertTrue(appURLs.isEmpty());
    }

    @Test
    public void onlyTestTransitiveDependencies()
    {
        boolean shouldAddOnlyDependencies = false;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isTestScope());

        assertThat(appURLs.size(), equalTo(1));
        URL url = appURLs.iterator().next();
        assertURL(url, rootArtifact);
    }

    @Test
    public void excludeRootOnlyProvidedDependencies()
    {
        boolean shouldAddOnlyDependencies = true;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isProvidedScope());

        assertThat(appURLs.size(), equalTo(2));
        List<URL> results = sortURLs(appURLs);
        assertURL(results.get(0), commonsCliArtifact);
        assertURL(results.get(1), dom4JArtifact);
    }

    @Test
    public void onlyProvidedDependenciesIncludingRootArtifact()
    {
        boolean shouldAddOnlyDependencies = false;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isProvidedScope());

        assertThat(appURLs.size(), equalTo(3));
        List<URL> results = sortURLs(appURLs);
        assertURL(results.get(0), commonsCliArtifact);
        assertURL(results.get(1), dom4JArtifact);
        assertURL(results.get(2), rootArtifact);
    }

    @Test
    public void excludeRootOnlyCompileDependencies()
    {
        boolean shouldAddOnlyDependencies = true;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isCompileScope());

        assertThat(appURLs.size(), equalTo(2));
        List<URL> results = sortURLs(appURLs);
        assertURL(results.get(0), gsonArtifact);
        assertURL(results.get(1), commonsLangArtifact);
    }

    @Test
    public void onlyCompileDependenciesIncludingRootArtifact()
    {
        boolean shouldAddOnlyDependencies = false;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isCompileScope());

        assertThat(appURLs.size(), equalTo(3));
        List<URL> results = sortURLs(appURLs);
        assertURL(results.get(0), gsonArtifact);
        assertURL(results.get(1), commonsLangArtifact);
        assertURL(results.get(2), rootArtifact);
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

        builder = new ClassLoaderURLsBuilder(urls, mavenMultiModuleMapping, dependencies);

        boolean shouldAddOnlyDependencies = true;
        boolean shouldAddTransitiveDepFromExcluded = true;
        Set<URL> appURLs = builder.buildClassLoaderURLs(shouldAddOnlyDependencies, shouldAddTransitiveDepFromExcluded, artifact -> artifact.equals(rootArtifact), dependency -> dependency.isCompileScope());

        assertThat(appURLs.size(), equalTo(1));
        URL url = appURLs.iterator().next();
        assertURL(url, dom4JArtifact);
    }

    private List<URL> sortURLs(Set<URL> appURLs)
    {
        return appURLs.stream().sorted((u1, u2) -> u1.getFile().compareTo(u2.getFile())).collect(Collectors.toList());
    }

    private void assertURL(URL url, MavenArtifact artifact)
    {
        assertThat(url.getFile(), containsString(artifact.getGroupIdAsPath()));
        assertThat(url.getFile(), containsString(artifact.getArtifactId()));
    }

    private MavenArtifact buildMavenArtifact(String groupId, String artifactId, String type, String version, String scope)
    {
        return new MavenArtifact(groupId, artifactId, type, version, scope);
    }

    private void buildDefaultURLs() throws MalformedURLException
    {
        urls = Sets.newHashSet();
        urls.add(buildRootArtifactURLMock());
        urls.add(buildCommonsLangArtifactURLMock());
        urls.add(buildCommonsCliArtifactURLMock());
        urls.add(buildDom4jCliArtifactURLMock());
        urls.add(buildGsonArtifactURLMock());
    }

    private URL buildRootArtifactURLMock() throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append(".m2").append(s).append("repository").append(s)
                .append("org").append(s)
                .append("my").append(s)
                .append("company").append(s)
                .append("core-artifact").append(s)
                .append("1.0.0").append(s)
                .append("core-artifact-1.0.0.jar");

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

    private URL buildCommonsLangArtifactURLMock() throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append(".m2").append(s).append("repository").append(s)
                .append("org").append(s)
                .append("apache").append(s)
                .append("commons").append(s)
                .append("commons-lang3").append(s)
                .append("3.4").append(s)
                .append("commons-lang3-3.4.jar");

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

    private URL buildGsonArtifactURLMock() throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append(".m2").append(s).append("repository").append(s)
                .append("com").append(s)
                .append("google").append(s)
                .append("code").append(s)
                .append("gson").append(s)
                .append("gson").append(s)
                .append("2.6.2").append(s)
                .append("gson-2.6.2.jar");

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

    private URL buildCommonsCliArtifactURLMock() throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append(".m2").append(s).append("repository").append(s)
                .append("commons-cli").append(s)
                .append("commons-cli").append(s)
                .append("1.2").append(s)
                .append("commons-cli-1.2.jar");

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

    private URL buildDom4jCliArtifactURLMock() throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append(".m2").append(s).append("repository").append(s)
                .append("dom4j").append(s)
                .append("dom4j").append(s)
                .append("1.6.1").append(s)
                .append("dom4j-1.6.1.jar");

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

    private void buildDefaultArtifacts()
    {
        rootArtifact = buildMavenArtifact("org.my.company", "core-artifact", "jar", "1.0.0", "compile");
        commonsLangArtifact = buildMavenArtifact("org.apache.commons", "commons-lang3", "jar", "3.4", "compile");
        gsonArtifact = buildMavenArtifact("com.google.code.gson", "gson", "jar", "2.6.2", "compile");
        commonsCliArtifact = buildMavenArtifact("commons-cli", "commons-cli", "jar", "1.2", "provided");
        dom4JArtifact = buildMavenArtifact("dom4j", "dom4j", "jar", "1.6.1", "provided");
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

        dependencies.put(rootArtifact, rootDependencies);
        dependencies.put(commonsCliArtifact, commonsCliDependencies);


        return dependencies;
    }
} 