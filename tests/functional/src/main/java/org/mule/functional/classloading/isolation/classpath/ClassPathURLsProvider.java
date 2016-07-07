/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classpath;

import java.net.URL;
import java.util.List;

/**
 * Resolves the classpath URLs.
 *
 * @since 4.0
 */
public interface ClassPathURLsProvider
{

    /**
     * @return a {@link List} of the classpath {@link URL}
     */
    List<URL> getURLs();
}
