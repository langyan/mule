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
 * Defines the list of URLS for each class loader that would be created in order to run the test.
 *
 * @since 4.0
 */
public class ArtifactUrlClassification
{

    private final List<URL> container;
    private final List<List<URL>> plugins;
    private final List<URL> application;

    public ArtifactUrlClassification(List<URL> container, List<List<URL>> plugins, List<URL> application)
    {
        this.container = container;
        this.plugins = plugins;
        this.application = application;
    }

    public List<URL> getContainer()
    {
        return container;
    }

    public List<List<URL>> getPlugins()
    {
        return plugins;
    }

    public List<URL> getApplication()
    {
        return application;
    }
}
