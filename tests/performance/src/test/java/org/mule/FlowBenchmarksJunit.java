/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.subscriber.SubmissionEmitter;

public class FlowBenchmarksJunit  extends AbstractMuleContextTestCase
{

    AsyncInterceptingMessageProcessor async;
    Flow flow;

    MessageProcessor listener;
    WorkManager workManager;

    MuleEvent event;
    AtomicLong counter;

    SubmissionEmitter<MuleEvent> submissionEmitter;
    EmitterProcessor<MuleEvent> emitterProcessor;



    Subscriber<? super MuleEvent> subscriber;

    //@Param({"synchronous","default"})//"queued-asynchronous"})
    //String processingStrategy;

    //@Param({"0", "16", "64", "256"})
    int bufferSize = 0;

    //@Param({"16", "64", "256", "1024"})
    int queueSize = 64;

    //@Param({"0", "4"})
    int waitStrategy = 0;

    /**
     * 1 = NONE
     * 2 = CPU
     * 3 = SLEEP
     */
    //@Param({"2", "3"})
    int workMode = 3;

    //@Param({"0", "1", "2", "4", "8"})
    int workTime = 5;

    @Before
    public void setup() throws Exception
    {

        flow = new Flow("flow", muleContext);
        listener = new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                if (workTime > 0)
                {
                    switch (workMode)
                    {
                        case 2:
                            Blackhole.consumeCPU(workTime * 1000000);
                            break;
                        case 3:
                            try
                            {
                                Thread.sleep(workTime);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        default:
                    }
                }
                return event;
            }

            @Override
            public boolean isBlocking()
            {
                return workMode == 3;
            }
        };
        //ProcessingStrategy ps = ProcessingStrategyUtils.parseProcessingStrategy(processingStrategy);
        //if (ps instanceof AsynchronousProcessingStrategy)
        //{
        //    ((AsynchronousProcessingStrategy) ps).setPoolExhaustedAction(waitStrategy);
        //    ((AsynchronousProcessingStrategy) ps).setMaxBufferSize(bufferSize);
        //}
        //if (ps instanceof QueuedAsynchronousProcessingStrategy)
        //{
        //    ((QueuedAsynchronousProcessingStrategy) ps).setMaxQueueSize(queueSize);
        //}
        //flow.setProcessingStrategy(ps);
        //flow.setProcessingStrategy(new ProcessingStrategy()
        //{
        //    @Override
        //    public void configureProcessors(List<MessageProcessor> processors, org.mule.runtime.core.api.processor
        //            .StageNameSource nameSource, MessageProcessorChainBuilder chainBuilder, MuleContext muleContext)
        //    {
        //        workQueueProcessor = WorkQueueProcessor.create();
        //        MessageProcessorChain chain = null;
        //        try
        //        {
        //            chain = new DefaultMessageProcessorChainBuilder().chain(processors).build();
        //        }
        //        catch (MuleException e)
        //        {
        //            e.printStackTrace();
        //        }
        //        for (int i = 0; i < 16; i++)
        //        {
        //            final MessageProcessorChain finalChain = chain;
        //            workQueueProcessor.subscribe(new Subscriber<MuleEvent>()
        //            {
        //                @Override
        //                public void onSubscribe(Subscription s)
        //                {
        //                    s.request(Long.MAX_VALUE);
        //                }
        //
        //                @Override
        //                public void onNext(MuleEvent event)
        //                {
        //                    try
        //                    {
        //                        finalChain.process(event);
        //                    }
        //                    catch (MuleException e)
        //                    {
        //                        e.printStackTrace();
        //                    }
        //                }
        //
        //                @Override
        //                public void onError(Throwable throwable)
        //                {
        //
        //                }
        //
        //                @Override
        //                public void onComplete()
        //                {
        //
        //                }
        //            });
        //        }
        //        chainBuilder.chain(new MessageProcessor()
        //        {
        //            @Override
        //            public MuleEvent process(MuleEvent event) throws MuleException
        //            {
        //                workQueueProcessor.onNext(event);
        //                return VoidMuleEvent.getInstance();
        //            }
        //        });
        //    }
        //});
        flow.setMessageProcessors(Collections.singletonList(listener));
        muleContext.getRegistry().registerFlowConstruct(flow);
        flow.start();

        event = new DefaultMuleEvent(new DefaultMuleMessage("Hello World", muleContext), MessageExchangePattern
                .ONE_WAY, flow);

        Flux.from(Flux.from(new Publisher<MuleEvent>()
        {
            @Override
            public void subscribe(Subscriber<? super MuleEvent> s)
            {
                FlowBenchmarksJunit.this.subscriber = s;
                s.onSubscribe(new Subscription()
                {
                    @Override
                    public void request(long n)
                    {

                    }

                    @Override
                    public void cancel()
                    {

                    }
                });
            }
        }).as(flow)).subscribe();

        //emitterProcessor = EmitterProcessor.<MuleEvent>create(256);//.connect();
        //Flux.from(emitterProcessor).subscribe();
        //submissionEmitter = emitterProcessor.connectEmitter();

    }

    @Test
    public void test() throws MuleException
    {
        //return Flux.from(Flux.just(event).as(flow)).subscribe();
        //flow.process(event);
        //System.out.println("EMIITTER");
        //for (int i = 0; i < 50000000; i++)
        //{
        //    submissionEmitter.submit(event);
        //}
        //System.out.println("SUBSCIBER");
        //for (int i = 0; i < 500000; i++)
        //{
        //if (processingStrategy.equals("default"))
        //{
        //    emitterProcessor.onNext(event);
        //}
        //else
        //{
            flow.process(event);
        //}
        //subscriber.onNext(event);
        //emitterProcessor.onNext(event);
    }

}
