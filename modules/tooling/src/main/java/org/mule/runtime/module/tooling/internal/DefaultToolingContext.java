/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;


import org.mule.runtime.module.tooling.api.ConnectionTestingService;
import org.mule.runtime.module.tooling.api.ToolingContext;
import org.mule.runtime.module.tooling.api.artifact.Artifact;

public class DefaultToolingContext implements ToolingContext
{

    private final ConnectionTestingService connectionTestingService;
    private final Artifact artifact;

    public DefaultToolingContext(Artifact artifact)
    {
        this.artifact = artifact;
        this.connectionTestingService = new DefaultConnectionTestingService(artifact);
    }

    public ConnectionTestingService getConnectionTestingService()
    {
        return connectionTestingService;
    }

}
