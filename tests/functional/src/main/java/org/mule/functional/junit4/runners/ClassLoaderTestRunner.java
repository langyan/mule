/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.util.List;

/**
 * Defines the different {@link ClassLoader}s for running the test.
 *
 * @since 4.0
 */
public final class ClassLoaderTestRunner
{

    private ArtifactClassLoader container;
    private List<ArtifactClassLoader> plugins;
    private ArtifactClassLoader application;

    public ClassLoaderTestRunner(ArtifactClassLoader container, List<ArtifactClassLoader> plugins, ArtifactClassLoader application)
    {
        this.container = container;
        this.plugins = plugins;
        this.application = application;
    }

    public ArtifactClassLoader getContainer()
    {
        return container;
    }

    public List<ArtifactClassLoader> getPlugins()
    {
        return plugins;
    }

    public ArtifactClassLoader getApplication()
    {
        return application;
    }

    public Class<?> loadClassWithApplicationClassLoader(String name)
    {
        try
        {
            return application.getClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Couldn't load class using application class loader", e);
        }
    }
}
