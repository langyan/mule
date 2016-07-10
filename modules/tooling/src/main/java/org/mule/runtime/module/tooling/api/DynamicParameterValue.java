/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api;

public class DynamicParameterValue
{

    private String parameterName;
    private Object parameterValue;

    public DynamicParameterValue(String parameterName, Object parameterValue)
    {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public String getParameterName()
    {
        return parameterName;
    }

    public Object getParameterValue()
    {
        return parameterValue;
    }
}
