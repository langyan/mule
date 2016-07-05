/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.util;

import java.util.function.Predicate;

/**
 * A {@link java.util.function.Predicate} that always returns true.
 */
public final class TruePredicate<T> implements Predicate<T>
{
    @Override
    public boolean test(T t)
    {
        return true;
    }
}
