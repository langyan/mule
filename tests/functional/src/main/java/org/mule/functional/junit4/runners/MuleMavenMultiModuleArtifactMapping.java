/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;

import java.io.IOException;
import java.util.Properties;

/**
 * Mule default implementation for getting modules based on artifactIds.
 *
 * @since 4.0
 */
public class MuleMavenMultiModuleArtifactMapping implements MavenMultiModuleArtifactMapping
{
    public static final String MAVEN_MODULE_MAPPING_PROPERTIES = "maven-module-mapping.properties";
    private Properties mappings;

    public MuleMavenMultiModuleArtifactMapping()
    {
        try
        {
            this.mappings = loadProperties(currentThread().getContextClassLoader().getResource(MAVEN_MODULE_MAPPING_PROPERTIES));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while loading '" + MAVEN_MODULE_MAPPING_PROPERTIES + "' properties");
        }
    }

    @Override
    public String mapModuleFolderNameFor(String artifactId)
    {
        return mappings.getProperty(artifactId);
    }

    @Override
    public String getMavenArtifactIdFor(String path)
    {
        for(Object propertyName : mappings.keySet())
        {
            String relativeFolder = (String) mappings.get(propertyName);
            if (path.endsWith(relativeFolder))
            {
                return (String) propertyName;
            }
        }
        throw new IllegalArgumentException("Couldn't find a mapping multi-module folder to get the artifactId for path: " + path);
    }

}
