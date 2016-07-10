/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import org.mule.runtime.module.repository.api.BundleDescriptor;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingContext;
import org.mule.runtime.module.tooling.api.ToolingContextBuilder;
import org.mule.runtime.module.tooling.api.artifact.Artifact;
import org.mule.runtime.module.tooling.api.artifact.ArtifactBuilder;
import org.mule.runtime.module.tooling.api.artifact.ArtifactBuilderFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultToolingContextBuilder implements ToolingContextBuilder
{

    private static final String EXTENSION_BUNDLE_TYPE = "zip";
    private final RepositoryService repositoryService;
    private final ArtifactBuilderFactory artifactBuilderFactory;
    private ClassLoader classLoader;
    private List<BundleDescriptor> bundleDescriptors = new ArrayList<>();

    DefaultToolingContextBuilder(RepositoryService repositoryService, ArtifactBuilderFactory artifactBuilderFactory)
    {
        this.artifactBuilderFactory = artifactBuilderFactory;
        this.repositoryService = repositoryService;
    }

    @Override
    public ToolingContextBuilder addExtension(String groupId, String artifactId, String artifactVersion)
    {
        this.bundleDescriptors.add(new BundleDescriptor.Builder()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setType(EXTENSION_BUNDLE_TYPE)
            .setVersion(artifactVersion).build());
        return this;
    }

    public ToolingContext build()
    {
        ArtifactBuilder artifactBuilder = artifactBuilderFactory.newBuilder();
        bundleDescriptors.stream()
                .forEach(bundleDescriptor -> artifactBuilder.addDependency(repositoryService.lookupBundle(bundleDescriptor)));

        Artifact artifact = artifactBuilder.build();

        return new DefaultToolingContext(artifact);

        //try
        //{
        //    MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        //    List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        //    builders.add()
        //    builders.add(new SpringXmlConfigurationBuilder(new String[] {}));
        //    //builders.add(new org.mule.runtime.module.launcher.application.ApplicationExtensionsManagerConfigurationBuilder())
        //    MuleContext muleContext = muleContextFactory.createMuleContext(builders, new DefaultMuleContextBuilder());
        //    muleContext.start();
        //    return new DefaultToolingContext(muleContext);
        //}
        //catch (Exception e)
        //{
        //    //TODO handle better exceptions
        //    throw new MuleRuntimeException(e);
        //}
    }

}
