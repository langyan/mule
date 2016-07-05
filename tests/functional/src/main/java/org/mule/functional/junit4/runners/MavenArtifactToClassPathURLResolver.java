/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.net.URL;
import java.util.List;

/**
 * Resolves a {@link MavenArtifact} by selecting from the list of URLs the one that matches
 *
 * @since 4.0
 */
public interface MavenArtifactToClassPathURLResolver
{

    /**
     * @param artifact to be used in order to find the {@link URL} in list of urls
     * @param urls a list of {@link URL}, most of the cases the one provided by the classpath
     * @throws IllegalArgumentException if the artifact couldn't be resolved to a URL
     * @return a non-null {@link URL} that represents the {@link MavenArtifact} passed
     */
    URL resolveURL(final MavenArtifact artifact, final List<URL> urls) throws IllegalArgumentException;
}
