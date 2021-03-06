/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.bulkexecute;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class BulkExecutePlaceholderTestCase extends AbstractBulkExecuteTestCase
{

    @Rule
    public SystemProperty bulkQuery = new SystemProperty("bulkQuery",  "update PLANET set NAME='Mercury' where POSITION=0;\n" +
                                                                       "update PLANET set NAME='Mercury' where POSITION=4");

    @Rule
    public SystemProperty file = new SystemProperty("file",  "integration/bulkexecute/bulk-execute.sql");

    public BulkExecutePlaceholderTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/bulkexecute/bulk-execute-placeholder-config.xml"};
    }

    @Test
    public void resolvesBulkQueryPlaceholder() throws Exception
    {
        doTest("bulkUpdatePlaceholder");
    }

    @Test
    public void resolvesFilePlaceholder() throws Exception
    {
        doTest("bulkUpdateFilePlaceholder");
    }

    private void doTest(String flowName) throws Exception
    {
        final MuleEvent responseEvent = flowRunner(flowName).withPayload(TEST_MESSAGE).run();

        final MuleMessage response = responseEvent.getMessage();
        assertBulkModeResult(response.getPayload());
    }
}
