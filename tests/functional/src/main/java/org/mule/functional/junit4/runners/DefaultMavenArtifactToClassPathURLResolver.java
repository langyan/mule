/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link MavenArtifactToClassPathURLResolver} that supports artifacts already packages (for CI environments)
 * and multi-module maven projects.
 *
 * @since 4.0
 */
public class DefaultMavenArtifactToClassPathURLResolver implements MavenArtifactToClassPathURLResolver
{
    private final MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping;

    public DefaultMavenArtifactToClassPathURLResolver(MavenMultiModuleArtifactMapping mavenMultiModuleArtifactMapping)
    {
        this.mavenMultiModuleArtifactMapping = mavenMultiModuleArtifactMapping;
    }

    @Override
    public URL resolveURL(final MavenArtifact artifact, final List<URL> urls)
    {
        Optional<URL> artifactURL = urls.stream().filter(filePath -> filePath.getFile().contains(artifact.getGroupIdAsPath() + File.separator + artifact.getArtifactId() + File.separator)).findFirst();
        if (artifactURL.isPresent())
        {
            return artifactURL.get();
        }
        else
        {
            return getModuleURL(artifact, urls, mavenMultiModuleArtifactMapping);
        }
    }

    private URL getModuleURL(final MavenArtifact artifact, final Collection<URL> urls, final MavenMultiModuleArtifactMapping mavenMultiModuleMapping)
    {
        final StringBuilder moduleFolder = new StringBuilder(mavenMultiModuleMapping.mapModuleFolderNameFor(artifact.getArtifactId())).append("target/");

        // Fix to handle when running test during an install phase due to maven builds the classpath pointing out to packaged files instead of classes folders.
        final StringBuilder explodedUrlSuffix = new StringBuilder();
        final StringBuilder packagedUrlSuffix = new StringBuilder();
        if (artifact.isTestScope() && artifact.getType().equals("test-jar"))
        {
            explodedUrlSuffix.append("test-classes/");
            packagedUrlSuffix.append(".*-tests.jar");
        }
        else
        {
            explodedUrlSuffix.append("classes/");
            packagedUrlSuffix.append("^(?!.*?(?:-tests.jar)).*.jar");
        }
        final Optional<URL> localFile = urls.stream().filter(url -> {
            String path = url.toString();
            if (path.contains(moduleFolder))
            {
                String pathSuffix = path.substring(path.lastIndexOf(moduleFolder.toString()) + moduleFolder.length(), path.length());
                return pathSuffix.matches(explodedUrlSuffix.toString()) || pathSuffix.matches(packagedUrlSuffix.toString());
            }
            return false;
        }).findFirst();
        if (!localFile.isPresent())
        {
            throw new IllegalArgumentException("Cannot locate artifact as multi-module dependency: '" + artifact + "', on module folder: " + moduleFolder + " using exploded url suffix regex: " + explodedUrlSuffix + " or " + packagedUrlSuffix);

        }
        return localFile.get();
    }

}
