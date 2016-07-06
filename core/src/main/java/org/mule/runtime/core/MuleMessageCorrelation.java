/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.core.util.StringUtils;

import java.io.Serializable;

public class MuleMessageCorrelation implements Serializable
{
    private static final String NOT_SET = "<not set>";

    private final String correlationId;
    private final Integer correlationGroupSize;
    private final Integer correlationSequence;

    public MuleMessageCorrelation(String corealationId, Integer correlationGroupSize, Integer correlationSequence)
    {
        this.correlationId = corealationId;
        this.correlationGroupSize = correlationGroupSize;
        this.correlationSequence = correlationSequence;
    }

    /**
     * Sets a correlationId for this message. The correlation Id can be used by components in the system to manage
     * message relations.
     * <p/>
     * The correlationId is associated with the message using the underlying transport protocol. As such not all
     * messages will support the notion of a correlationId i.e. tcp or file. In this situation the correlation Id is set
     * as a property of the message where it's up to developer to keep the association with the message. For example if
     * the message is serialised to xml the correlationId will be available in the message.
     *
     * @return the correlationId for this message or null if one hasn't been set
     */
    public String getId()
    {
        return correlationId;
    }

    /**
     * Gets the sequence or ordering number for this message in the the correlation group (as defined by the
     * correlationId)
     *
     * @return the sequence number or null if the sequence is not important
     */
    public Integer getSequence()
    {
        return correlationSequence;
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @return total messages in this group or null if the size is not known
     */
    public Integer getGroupSize()
    {
        return correlationGroupSize;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(120);

        // format message for multi-line output, single-line is not readable
        buf.append("{");
        buf.append(" id=").append(StringUtils.defaultString(getId(), NOT_SET));
        buf.append("; groupSize=").append(getGroupSize());
        buf.append("; sequence=").append(getSequence());
        buf.append('}');
        return buf.toString();
    }
}
