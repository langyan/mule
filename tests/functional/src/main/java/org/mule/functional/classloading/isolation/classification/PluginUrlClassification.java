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
 * Defines the list of URLS for the plugin class loader that would be created in order to run the test and
 * also has the reference to the extension class.
 *
 * @since 4.0
 */
public class PluginUrlClassification
{
    private List<URL> urls;
    private String name;

    public PluginUrlClassification(String name, List<URL> urls)
    {
        this.name = name;
        this.urls = urls;
    }

    public List<URL> getUrls()
    {
        return urls;
    }

    public String getName()
    {
        return name;
    }
}
