/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.context.notification.ComponentMessageNotification;

/**
 * <code>ComponentMessageNotificationListener</code> is an observer interface that objects
 * can use to receive notifications about messages being processed by components
 */
public interface ComponentMessageNotificationListener<T extends ComponentMessageNotification> extends ServerNotificationListener<ComponentMessageNotification>
{
    // no extra methods
}
