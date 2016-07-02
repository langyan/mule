/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for runnner.
 */
public final class RunnerModuleUtils
{
    public static final String EXCLUDED_PROPERTIES_FILE = "excluded.properties";

    private RunnerModuleUtils()
    {
    }

    /**
     * Loads the excluded.properties file.
     *
     * @return a {@link Properties} loaded with excluded.properties file.
     * @throws IOException if the properties couldn't load the file.
     * @throws IllegalStateException if the excluded.properties file couldn't be found.
     */
    public static final Properties getExcludedProperties() throws IllegalStateException, IOException
    {
        InputStream excludedPropertiesURL = RunnerModuleUtils.class.getClassLoader().getResourceAsStream(EXCLUDED_PROPERTIES_FILE);
        if (excludedPropertiesURL == null)
        {
            throw new IllegalStateException("Couldn't find file: " + EXCLUDED_PROPERTIES_FILE + " in classpath, at least one should be defined");
        }
        Properties excludedProperties = new Properties();
        excludedProperties.load(excludedPropertiesURL);
        return excludedProperties;
    }
}
