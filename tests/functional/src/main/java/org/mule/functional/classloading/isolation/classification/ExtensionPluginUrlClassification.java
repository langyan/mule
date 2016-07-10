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
 * Represents a URL classification for an {@link org.mule.runtime.extension.api.annotation.Extension}
 *
 * @since 4.0
 */
public class ExtensionPluginUrlClassification extends PluginUrlClassification
{

    private Class extension;

    public ExtensionPluginUrlClassification(Class extension, List<URL> urls)
    {
        super(extension.getName(), urls);
        this.extension = extension;
    }

    public Class getExtension()
    {
        return extension;
    }
}
