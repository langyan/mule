/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.OperationContext;

/**
 * An implementation of {@link ArgumentResolver} which resolves
 * to a parameter value of name {@link #parameterName}
 *
 * @param <T> the type of the argument to be resolved
 * @since 3.7.0
 */
public class ByParameterNameArgumentResolver<T> implements ArgumentResolver<T>
{

    private final String parameterName;

    public ByParameterNameArgumentResolver(String parameterName)
    {
        this.parameterName = parameterName;
    }

    /**
     * {@inheritDoc}
     *
     * @param operationContext an {@link OperationContext}
     * @return the result of invoking {@link OperationContext#getParameter(String)} with {@link #parameterName}
     */
    @Override
    public T resolve(OperationContext operationContext)
    {
        if (operationContext.hasParameter(parameterName))
        {
            return (T) operationContext.getParameter(parameterName);
        }
        else
        {
            return null;
        }
    }
}
