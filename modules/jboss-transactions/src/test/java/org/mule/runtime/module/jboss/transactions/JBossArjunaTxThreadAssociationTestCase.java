/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.jboss.transactions;

import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.tck.AbstractTxThreadAssociationTestCase;

public class JBossArjunaTxThreadAssociationTestCase extends AbstractTxThreadAssociationTestCase
{

    protected TransactionManagerFactory getTransactionManagerFactory()
    {
        return new JBossArjunaTransactionManagerFactory();
    }
}
