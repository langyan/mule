/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.MuleModule;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleClassLoaderLookupPolicy;

import com.google.common.collect.ImmutableSet;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Extends the default {@link ContainerClassLoaderFactory} for testing in order to add boot packages and build
 * a {@link ClassLoader} for the container that do resolves classes instead of delegating to its parent and also
 * allows to create the container {@link ClassLoaderLookupPolicy} based on a {@link ClassLoader}.
 *
 * @since 4.0
 */
public class TestContainerClassLoaderFactory extends ContainerClassLoaderFactory
{

    private Set<String> extraBootPackages;
    private URL[] urls;

    public TestContainerClassLoaderFactory(Set<String> extraBootPackages, URL[] urls)
    {
        this.extraBootPackages = extraBootPackages;
        this.urls = urls;
    }

    @Override
    protected ArtifactClassLoader createArtifactClassLoader(ClassLoader parentClassLoader, List<MuleModule> muleModules, ClassLoaderLookupPolicy containerLookupPolicy)
    {
        final ArtifactClassLoader containerClassLoader = new MuleArtifactClassLoader("mule", urls, parentClassLoader, new MuleClassLoaderLookupPolicy(Collections.emptyMap(), getBootPackages()));
        return createContainerFilteringClassLoader(muleModules, containerClassLoader);
    }

    @Override
    public Set<String> getBootPackages()
    {
        return ImmutableSet.<String>builder().addAll(super.getBootPackages()).addAll(extraBootPackages).build();
    }

    public ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy(ClassLoader classLoader)
    {
        return super.getContainerClassLoaderLookupPolicy(discoverModules(classLoader));
    }

    private List<MuleModule> discoverModules(ClassLoader classLoader)
    {
        return new ClasspathModuleDiscoverer(classLoader).discover();
    }

}
