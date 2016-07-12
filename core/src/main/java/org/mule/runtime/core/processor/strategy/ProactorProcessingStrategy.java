/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.reactivestreams.Publisher;

public class ProactorProcessingStrategy extends NonBlockingProcessingStrategy
{

    private ExecutorService eventLoop = Executors.newSingleThreadExecutor();


    public Function<Publisher<MuleEvent>, Publisher<MuleEvent>> onFlow(Pipeline pipeline,
                                                                       Function<Publisher<MuleEvent>,
                                                                               Publisher<MuleEvent>> publisherFunction)
    {
        return publisher -> from(publisher).publishOn(eventLoop).as(publisherFunction);
    }

    public Function<Publisher<MuleEvent>, Publisher<MuleEvent>> onProcessor(MessageProcessor messageProcessor,
                                                                            Function<Publisher<MuleEvent>,
            Publisher<MuleEvent>>
            publisherFunction)
    {
        if (messageProcessor.isBlocking())
        {
            return publisher -> from(publisher).publishOn(executorService).compose(publisherFunction).publishOn
                    (eventLoop);
        }
        else
        {
            return publisher -> publisher;
        }
    }
}
