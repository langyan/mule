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

    private ArtifactClassLoader containerClassLoader;
    private List<ArtifactClassLoader> pluginClassLoaders;
    private ArtifactClassLoader applicationClassLoader;

    public ClassLoaderTestRunner(ArtifactClassLoader containerClassLoader, List<ArtifactClassLoader> pluginClassLoaders, ArtifactClassLoader applicationClassLoader)
    {
        this.containerClassLoader = containerClassLoader;
        this.pluginClassLoaders = pluginClassLoaders;
        this.applicationClassLoader = applicationClassLoader;
    }

    public ArtifactClassLoader getContainerClassLoader()
    {
        return containerClassLoader;
    }

    public List<ArtifactClassLoader> getPluginClassLoaders()
    {
        return pluginClassLoaders;
    }

    public ArtifactClassLoader getApplicationClassLoader()
    {
        return applicationClassLoader;
    }

    public Class<?> loadClassWithApplicationClassLoader(String name) throws ClassNotFoundException
    {
        return applicationClassLoader.getClassLoader().loadClass(name);
    }
}
