/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.Lists;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test for {@link MavenArtifactToClassPathURLResolver}
 */
@SmallTest
public class DefaultMavenArtifactToClassPathURLResolverTest
{

    public static final String PARENT_PROJECT_FOLDER = "/parent-project/";
    public static final String UTILS_CORE_MODULE_FOLDER = PARENT_PROJECT_FOLDER + "utils/";

    private DefaultMavenArtifactToClassPathURLResolver urlResolver;
    private MavenMultiModuleArtifactMapping mapping;

    private MavenArtifact coreArtifact;
    private MavenArtifact utilsCoreArtifact;
    private URL coreArtifactMavenRepoURL;
    private URL utilsCoreArtifactMultiModuleURL;

    private MavenArtifact commonCliArtifact;

    @Rule
    public ExpectedException expectedException = none();

    @Before
    public void before() throws Exception
    {
        mapping = mock(MavenMultiModuleArtifactMapping.class);
        urlResolver = new DefaultMavenArtifactToClassPathURLResolver(mapping);

        commonCliArtifact = new MavenArtifact("commons-cli", "commons-cli", "jar", "1.2", "provided");
        coreArtifact = new MavenArtifact("org.my.company", "core-artifact", "jar", "1.0.0", "compile");
        coreArtifactMavenRepoURL = buildArtifactURLMock(coreArtifact);

        utilsCoreArtifact = new MavenArtifact("org.my.company", "utils", "jar", "1.0.0", "compile");
        utilsCoreArtifactMultiModuleURL = buildMultiModuleURLMock(UTILS_CORE_MODULE_FOLDER);
    }

    @Test
    public void resolveURLUsingGA() throws Exception
    {
        assertURL(coreArtifact, Lists.newArrayList(buildArtifactURLMock(commonCliArtifact), coreArtifactMavenRepoURL), coreArtifactMavenRepoURL);
        verifyZeroInteractions(mapping);
    }

    @Test
    public void resolveUrlMultiModuleMapping() throws Exception
    {
        when(mapping.mapModuleFolderNameFor(utilsCoreArtifact.getArtifactId())).thenReturn(UTILS_CORE_MODULE_FOLDER);
        assertURL(utilsCoreArtifact, Lists.newArrayList(buildArtifactURLMock(commonCliArtifact), coreArtifactMavenRepoURL, utilsCoreArtifactMultiModuleURL), utilsCoreArtifactMultiModuleURL);
        verify(mapping);
    }

    @Test
    public void urlNotFoundForArtifact() throws Exception
    {
        when(mapping.mapModuleFolderNameFor(coreArtifact.getArtifactId())).thenReturn("doesnotexist-folder");
        expectedException.expect(IllegalArgumentException.class);
        assertURL(coreArtifact, Lists.newArrayList(buildArtifactURLMock(commonCliArtifact)), coreArtifactMavenRepoURL);
    }


    private void assertURL(MavenArtifact artifact, List<URL> urls, URL expectedURL) throws MalformedURLException
    {
        URL resolvedURL = urlResolver.resolveURL(artifact, urls);
        assertThat(resolvedURL, equalTo(expectedURL));
    }

    private URL buildArtifactURLMock(MavenArtifact artifact) throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append(".m2").append(s).append("repository").append(s)
                .append(artifact.getGroupIdAsPath()).append(s)
                .append(artifact.getArtifactId()).append(s)
                .append(artifact.getVersion()).append(s)
                .append(artifact.getArtifactId()).append("-").append(artifact.getVersion()).append(".").append(artifact.getType());

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

    private URL buildMultiModuleURLMock(String multiModuleFolder) throws MalformedURLException
    {
        String s = File.separator;
        StringBuilder filePath = new StringBuilder();
        filePath.append(s).append("home").append(s).append("user").append(s).append("workspace")
                .append(multiModuleFolder)
                .append("target").append(s).append("classes").append(s);

        URL artifactURL = new URL("file", "", -1, filePath.toString());

        return artifactURL;
    }

}
