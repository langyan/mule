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
    private Class extension;
    private List<URL> urls;

    public PluginUrlClassification(Class extension, List<URL> urls)
    {
        this.extension = extension;
        this.urls = urls;
    }

    public Class getExtension()
    {
        return extension;
    }

    public List<URL> getUrls()
    {
        return urls;
    }
}
