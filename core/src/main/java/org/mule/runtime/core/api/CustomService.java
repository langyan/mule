/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;

public class CustomService
{
    private Optional<Class> serviceClass;
    private Optional<Object> serviceImpl;

    public CustomService(Class serviceClass)
    {
        this.serviceClass = of(serviceClass);
        this.serviceImpl = empty();
    }

    public CustomService(Object serviceImpl)
    {
        this.serviceImpl = of(serviceImpl);
        this.serviceClass = empty();
    }

    public Optional<Class> getServiceClass()
    {
        return serviceClass;
    }

    public Optional<Object> getServiceImpl()
    {
        return serviceImpl;
    }

}
