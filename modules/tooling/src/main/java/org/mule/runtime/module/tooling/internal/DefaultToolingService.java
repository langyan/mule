/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingContextBuilder;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.artifact.ArtifactBuilderFactory;

public class DefaultToolingService implements ToolingService
{

    private final ArtifactBuilderFactory artifactBuilderFactory;
    private RepositoryService repositoryService;

    public DefaultToolingService(RepositoryService repositoryService, ArtifactBuilderFactory artifactBuilderFactory)
    {
        this.repositoryService = repositoryService;
        this.artifactBuilderFactory = artifactBuilderFactory;
    }

    @Override
    public ToolingContextBuilder newToolingContextBuilder()
    {
        return new DefaultToolingContextBuilder(repositoryService, artifactBuilderFactory);
    }
}
