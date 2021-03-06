/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http.functional;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpOutboundDataTypeTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "http-datatype-config.xml";
    }

    @Test
    public void propagatesDataType() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage muleMessage = MuleMessage.builder().payload(TEST_MESSAGE).addOutboundProperty(
        CONTENT_TYPE_PROPERTY, MediaType.TEXT + "; charset=" + UTF_16.name()).build();

        client.dispatch("vm://testInput", muleMessage);

        MuleMessage response = client.request("vm://testOutput", 120000);

        assertThat(response.getDataType().getMediaType().getPrimaryType(), equalTo(MediaType.TEXT.getPrimaryType()));
        assertThat(response.getDataType().getMediaType().getSubType(), equalTo(MediaType.TEXT.getSubType()));
        assertThat(response.getDataType().getMediaType().getCharset().get(), equalTo(UTF_16));
        assertThat(response.getOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY), is(nullValue()));
        assertThat(response.getOutboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY), is(nullValue()));
    }
}
