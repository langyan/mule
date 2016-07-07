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

    private final List<URL> container;
    private final List<PluginUrlClassification> pluginUrlClassifications;
    private final List<URL> application;

    /**
     * Creates a instance with the list of urls classified in container, plugins and application.
     *
     * @param container
     * @param pluginUrlClassifications
     * @param application
     */
    public ArtifactUrlClassification(List<URL> container, List<PluginUrlClassification> pluginUrlClassifications, List<URL> application)
    {
        this.container = container;
        this.pluginUrlClassifications = pluginUrlClassifications;
        this.application = application;
    }

    public List<URL> getContainerURLs()
    {
        return container;
    }

    public List<PluginUrlClassification> getPluginClassificationURLs()
    {
        return pluginUrlClassifications;
    }

    public List<URL> getApplicationURLs()
    {
        return application;
    }
}
