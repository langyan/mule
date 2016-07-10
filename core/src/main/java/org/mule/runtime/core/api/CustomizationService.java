/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import java.util.Optional;

//TODO should we already add methods to add other services?
public interface CustomizationService
{

    <T> void customizeServiceImpl(String serviceId, T serviceImpl);

    <T> void customizeServiceClass(String serviceId, Class<T> serviceClass);

    Optional<CustomService> getCustomizedService(String serviceId);

}
