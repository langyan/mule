/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import java.net.URL;
import java.util.List;

/**
 * Defines the list of URLS for each class loader that would be created in order to run the test.
 * It is the result of {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ArtifactUrlClassification
{

    private final List<URL> containerURLs;
    private final List<PluginUrlClassification> pluginUrlClassificationsURLs;
    private final List<URL> applicationURLs;

    /**
     * Creates a instance with the list of {@link URL}s classified in container, plugins and application.
     *
     * @param containerURLs list of {@link URL} that define the artifacts that would be loaded with the container {@link ClassLoader}
     * @param pluginUrlClassificationsURLs for each plugin discovered a list of {@link URL} that define the artifacts that would be loaded by the plugin {@link ClassLoader}
     * @param applicationURLs list of {@link URL} that define the artifacts that would be loaded with the application {@link ClassLoader}
     */
    public ArtifactUrlClassification(List<URL> containerURLs, List<PluginUrlClassification> pluginUrlClassificationsURLs, List<URL> applicationURLs)
    {
        this.containerURLs = containerURLs;
        this.pluginUrlClassificationsURLs = pluginUrlClassificationsURLs;
        this.applicationURLs = applicationURLs;
    }

    public List<URL> getContainerURLs()
    {
        return containerURLs;
    }

    public List<PluginUrlClassification> getPluginClassificationURLs()
    {
        return pluginUrlClassificationsURLs;
    }

    public List<URL> getApplicationURLs()
    {
        return applicationURLs;
    }
}
