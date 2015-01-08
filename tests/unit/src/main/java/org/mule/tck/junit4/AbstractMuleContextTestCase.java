/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.MuleTransportDescriptorService;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ServiceType;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.bootstrap.MuleRegistryBootstrapService;
import org.mule.config.bootstrap.RegistryBootstrapService;
import org.mule.config.bootstrap.RegistryBootstrapServiceUtil;
import org.mule.config.builders.ConfigurationBuilderService;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.config.builders.MuleConfigurationBuilderService;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.notification.MuleContextNotification;
import org.mule.osgi.DefaultTransportServiceDescriptorFactory;
import org.mule.osgi.MuleCoreActivator;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.TestingWorkListener;
import org.mule.tck.TriggerableMessageSource;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Extends {@link AbstractMuleTestCase} providing access to a {@link MuleContext}
 * instance and tools for manage it.
 */
public abstract class AbstractMuleContextTestCase extends AbstractMuleTestCase
{

    public static final String TEST_PAYLOAD = "test";
    public static final String WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY = "workingDirectory";

    public TemporaryFolder workingDirectory = new TemporaryFolder();

    /**
     * Top-level directories under <code>.mule</code> which are not deleted on each
     * test case recycle. This is required, e.g. to play nice with transaction manager
     * recovery service object store.
     */
    public static final String[] IGNORED_DOT_MULE_DIRS = new String[]{"transaction-log"};

    /**
     * If the annotations module is on the classpath, also enable annotations config builder
     */
    public static final String CLASSNAME_ANNOTATIONS_CONFIG_BUILDER = "org.mule.config.AnnotationsConfigurationBuilder";

    /**
     * The context used to run this test. Context will be created per class
     * or per method depending on  {@link #disposeContextPerClass}.
     * The context will be started only when {@link #startContext} is true.
     */
    protected static MuleContext muleContext;

    /**
     * Start the muleContext once it's configured (defaults to false for AbstractMuleTestCase, true for FunctionalTestCase).
     */
    private boolean startContext = false;

    /**
     * Convenient test message for unit testing.
     */
    public static final String TEST_MESSAGE = "Test Message";

    /**
     * Default timeout for multithreaded tests (using CountDownLatch, WaitableBoolean, etc.),
     * in milliseconds.  The higher this value, the more reliable the test will be, so it
     * should be set high for Continuous Integration.  However, this can waste time during
     * day-to-day development cycles, so you may want to temporarily lower it while debugging.
     */
    public static final long LOCK_TIMEOUT = 30000;

    /**
     * Default timeout for waiting for responses
     */
    public static final int RECEIVE_TIMEOUT = 5000;

    /**
     * Use this as a semaphore to the unit test to indicate when a callback has successfully been called.
     */
    protected Latch callbackCalled;

    /**
     * Indicates if the context should be instantiated per context. Default is
     * false, which means that a context will be instantiated per test method.
     */
    private boolean disposeContextPerClass;
    private BundleContext bundleContext;
    private MuleTransportDescriptorService transportDescriptorService;
    private ConfigurationBuilderService configurationBuilderService;

    protected RegistryBootstrapService registryBootstrapService;

    protected boolean isDisposeContextPerClass()
    {
        return disposeContextPerClass;
    }

    protected void setDisposeContextPerClass(boolean val)
    {
        disposeContextPerClass = val;
    }

    @Before
    public final void setUpMuleContext() throws Exception
    {
        bundleContext = getBundleContext();

        //TODO(pablo.kraan): OSGi - hack to set the global reference for tests running with no OSGi container
        if (MuleCoreActivator.bundleContext == null)
        {
            MuleCoreActivator.bundleContext = bundleContext;
        }

        workingDirectory.create();
        String workingDirectoryOldValue = System.setProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY, workingDirectory.getRoot().getAbsolutePath());
        try
        {
            doSetUpBeforeMuleContextCreation();

            muleContext = createMuleContext();

            if (isStartContext() && muleContext != null && !muleContext.isStarted())
            {
                startMuleContext();
            }

            doSetUp();
        }
        finally
        {
            if (workingDirectoryOldValue != null)
            {
                System.setProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY, workingDirectoryOldValue);
            }
            else
            {
                System.clearProperty(WORKING_DIRECTORY_SYSTEM_PROPERTY_KEY);
            }
        }
    }

    protected BundleContext getBundleContext()
    {
        return new BundleContextBuilder().build();
    }

    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
    }

    private void startMuleContext() throws MuleException, InterruptedException
    {
        final AtomicReference<Latch> contextStartedLatch = new AtomicReference<Latch>();

        contextStartedLatch.set(new Latch());
        muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {
            @Override
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                {
                    contextStartedLatch.get().countDown();
                }
            }
        });

        muleContext.start();

        contextStartedLatch.get().await(20, TimeUnit.SECONDS);
    }

    /**
     * Enables the adding of extra behavior on the set up stage of a test right
     * after the creation of the mule context in {@link #setUpMuleContext}.
     * <p>
     * Under normal circumstances this method could be replaced by a
     * <code>@Before</code> annotated method.
     *
     * @throws Exception if something fails that should halt the test case
     */
    protected void doSetUp() throws Exception
    {
        // template method
    }

    protected MuleContext createMuleContext() throws Exception
    {
        // Should we set up the manager for every method?
        MuleContext context;
        if (isDisposeContextPerClass() && muleContext != null)
        {
            context = muleContext;
        }
        else
        {
            //TODO(pablo.kraan): OSGi - move this initialization to somewhere else
            registryBootstrapService = createRegistryBootstrapService();
            configureRegistryBootstrapService(registryBootstrapService);

            transportDescriptorService = createTransportDescriptorService();
            configureTransportDescriptorService(transportDescriptorService);

            configurationBuilderService = createConfigurationBuilderService();
            configureConfigurationBuilderService(configurationBuilderService);

            DefaultMuleContextFactory muleContextFactory = createMuleContextFactory();
            List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
            builders.add(new SimpleConfigurationBuilder(getStartUpProperties()));
            //TODO(pablo.kraan): OSGi - CLASSNAME_ANNOTATIONS_CONFIG_BUILDER is not in the classpath anymore.
            //If the annotations module is on the classpath, add the annotations config builder to the list
            //This will enable annotations config for this instance
            //if (ClassUtils.isClassOnPath(CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
            //{
            //    builders.add((ConfigurationBuilder) ClassUtils.instanciateClass(CLASSNAME_ANNOTATIONS_CONFIG_BUILDER,
            //                                                                    ClassUtils.NO_ARGS, getClass()));
            //}
            builders.add(getBuilder());
            addBuilders(builders);
            DefaultMuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            contextBuilder.setTransportDescriptorService(transportDescriptorService);
            DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();
            String workingDirectory = this.workingDirectory.getRoot().getAbsolutePath();
            logger.info("Using working directory for test: " + workingDirectory);
            muleConfiguration.setWorkingDirectory(workingDirectory);
            contextBuilder.setMuleConfiguration(muleConfiguration);
            contextBuilder.setRegistryBootstrapService(registryBootstrapService);
            configureMuleContext(contextBuilder);
            context = muleContextFactory.createMuleContext(builders, contextBuilder);
            if (!isGracefulShutdown())
            {
                ((DefaultMuleConfiguration) context.getConfiguration()).setShutdownTimeout(0);
            }
        }
        return context;
    }

    protected void configureConfigurationBuilderService(ConfigurationBuilderService configurationBuilderService)
    {

    }

    protected ConfigurationBuilderService createConfigurationBuilderService()
    {
        return new MuleConfigurationBuilderService();
    }

    protected void configureTransportDescriptorService(MuleTransportDescriptorService transportDescriptorService)
    {
        try
        {
            //TODO(pablo.kraan): OSGi - tests should register transports only when they need them. Find a way to make that
            // easy to do without requiring a complex test case hierarchy
            //TODO(pablo.kraan): OSGi - added spring-core as a dependency just to have access to this class
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] transports = resolver.getResources("classpath*:/" + SpiUtils.SERVICE_ROOT + SpiUtils.PROVIDER_SERVICE_PATH+ "*.properties");

            for (Resource transport : transports)
            {
                String baseName = FilenameUtils.getBaseName(transport.getFilename());
                registerTransport(transportDescriptorService, baseName);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to load transport descriptor", e);
        }
    }

    private void registerTransport(MuleTransportDescriptorService transportDescriptorService, String transport)
    {
        DefaultTransportServiceDescriptorFactory testTransportServiceDescriptorFactory = new DefaultTransportServiceDescriptorFactory(transport, SpiUtils.findServiceDescriptor(ServiceType.TRANSPORT, transport));
        transportDescriptorService.registerDescriptorFactory(transport, testTransportServiceDescriptorFactory);
    }

    protected DefaultMuleContextFactory createMuleContextFactory()
    {
        DefaultMuleContextFactory defaultMuleContextFactory = new DefaultMuleContextFactory();
        defaultMuleContextFactory.setBundleContext(bundleContext);
        defaultMuleContextFactory.setTransportDescriptorService(transportDescriptorService);
        defaultMuleContextFactory.setConfigurationBuilderService(configurationBuilderService);

        return defaultMuleContextFactory;
    }

    protected MuleTransportDescriptorService createTransportDescriptorService()
    {
        return new MuleTransportDescriptorService();
    }

    protected RegistryBootstrapService createRegistryBootstrapService()
    {
        return new MuleRegistryBootstrapService();
    }

    protected void configureRegistryBootstrapService(RegistryBootstrapService registryBootstrapService)
    {
        RegistryBootstrapServiceUtil.configureUsingClassPath(registryBootstrapService);
    }


    //This sohuldn't be needed by Test cases but can be used by base testcases that wish to add further builders when
    //creating the MuleContext.
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        //No op
    }

    /**
     * Override this method to set properties of the MuleContextBuilder before it is
     * used to create the MuleContext.
     */
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        contextBuilder.setWorkListener(new TestingWorkListener());
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new DefaultsConfigurationBuilder();
    }

    protected String getConfigurationResources()
    {
        return StringUtils.EMPTY;
    }

    protected Properties getStartUpProperties()
    {
        return null;
    }

    @After
    public final void disposeContextPerTest() throws Exception
    {
        doTearDown();

        if (!isDisposeContextPerClass())
        {
            disposeContext();
            doTearDownAfterMuleContextDispose();
        }

        //When an Assumption fails then junit doesn't call @Before methods so we need to avoid executing delete if there's no root folder.
        workingDirectory.delete();
    }

    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
    }

    @AfterClass
    public static void disposeContext()
    {
        try
        {
            if (muleContext != null && !(muleContext.isDisposed() || muleContext.isDisposing()))
            {
                muleContext.dispose();

                MuleConfiguration configuration = muleContext.getConfiguration();

                if (configuration != null)
                {
                    final String workingDir = configuration.getWorkingDirectory();
                    // do not delete TM recovery object store, everything else is good to
                    // go
                    FileUtils.deleteTree(FileUtils.newFile(workingDir), IGNORED_DOT_MULE_DIRS);
                }
            }
            FileUtils.deleteTree(FileUtils.newFile("./ActiveMQ"));
        }
        finally
        {
            muleContext = null;
        }
    }

    /**
     * Enables the adding of extra behavior on the tear down stage of a test
     * before the mule context is disposed in {@link #disposeContextPerTest}.
     * <p>
     * Under normal circumstances this method could be replace with a
     * <code>@After</code> annotated method.
     *
     * @throws Exception if something fails that should halt the testcase
     */
    protected void doTearDown() throws Exception
    {
        // template method
    }

    public static InboundEndpoint getTestInboundEndpoint(String name) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext);
    }

    public static InboundEndpoint getTestInboundEndpoint(MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(mep, muleContext);
    }

    public static InboundEndpoint getTestTransactedInboundEndpoint(MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestTransactedInboundEndpoint(mep, muleContext);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, String uri) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, null, null, null, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, null, null, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, List<Transformer> transformers) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, null, transformers, null, null, null);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, List<Transformer> transformers) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, null, transformers, null, null);
    }

    public static InboundEndpoint getTestInboundEndpoint(String name, String uri,
                                                         List<Transformer> transformers, Filter filter, Map<Object, Object> properties, Connector connector) throws Exception
    {
        return MuleTestUtils.getTestInboundEndpoint(name, muleContext, uri, transformers, filter, properties, connector);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri,
                                                           List<Transformer> transformers, Filter filter, Map<Object, Object> properties) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties);
    }

    public static OutboundEndpoint getTestOutboundEndpoint(String name, String uri,
                                                           List<Transformer> transformers, Filter filter, Map<Object, Object> properties, Connector connector) throws Exception
    {
        return MuleTestUtils.getTestOutboundEndpoint(name, muleContext, uri, transformers, filter, properties, connector);
    }

    /**
     * @return creates a new {@link org.mule.api.MuleMessage} with a test payload
     */
    protected MuleMessage getTestMuleMessage()
    {
        return new DefaultMuleMessage(TEST_PAYLOAD, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, FlowConstruct service) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, service, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, FlowConstruct service, MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, service, mep, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, MuleContext muleContext) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEvent getTestEvent(Object data) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, mep, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, MuleSession session) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, session, muleContext);
    }

    public static MuleEventContext getTestEventContext(Object data) throws Exception
    {
        return MuleTestUtils.getTestEventContext(data, MessageExchangePattern.REQUEST_RESPONSE, muleContext);
    }

    public static MuleEventContext getTestEventContext(Object data, MessageExchangePattern mep) throws Exception
    {
        return MuleTestUtils.getTestEventContext(data, mep, muleContext);
    }

    public static Transformer getTestTransformer() throws Exception
    {
        return MuleTestUtils.getTestTransformer();
    }

    public static MuleEvent getTestEvent(Object data, InboundEndpoint endpoint) throws Exception
    {
        return MuleTestUtils.getTestEvent(data, endpoint, muleContext);
    }

    public static MuleEvent getTestEvent(Object data, Flow flow, InboundEndpoint endpoint)
            throws Exception
    {
        return MuleTestUtils.getTestEvent(data, flow, endpoint, muleContext);
    }

    public static MuleSession getTestSession(Flow flow, MuleContext context)
    {
        return MuleTestUtils.getTestSession(flow, context);
    }

    public static TestConnector getTestConnector() throws Exception
    {
        return MuleTestUtils.getTestConnector(muleContext);
    }

    public static Flow getTestFlow() throws Exception
    {
        return MuleTestUtils.getTestFlow(muleContext);
    }

    public static Flow getTestFlow(String name, Class<?> clazz) throws Exception
    {
        return MuleTestUtils.getTestFlow(name, clazz, muleContext);
    }

    public static Flow getTestFlow(String name, Class<?> clazz, Map<?, ?> props) throws Exception
    {
        return MuleTestUtils.getTestFlow(name, clazz, props, muleContext);
    }

    public static Flow getTestFlow(Object component) throws Exception
    {
        return MuleTestUtils.getTestFlow(MuleTestUtils.APPLE_FLOW, component, false, muleContext);
    }

    protected boolean isStartContext()
    {
        return startContext;
    }

    protected void setStartContext(boolean startContext)
    {
        this.startContext = startContext;
    }

    /**
     * Determines if the test case should perform graceful shutdown or not.
     * Default is false so that tests run more quickly.
     */
    protected boolean isGracefulShutdown()
    {
        return false;
    }

    /**
     * Create an object of instance <code>clazz</code>. It will then register the object with the registry so that any
     * dependencies are injected and then the object will be initialised.
     * Note that if the object needs to be configured with additional state that cannot be passed into the constructor you should
     * create an instance first set any additional data on the object then call {@link #initialiseObject(Object)}.
     *
     * @param clazz the class to create an instance of.
     * @param <T>   Object of this type will be returned
     * @return an initialised instance of <code>class</code>
     * @throws Exception if there is a problem creating or initializing the object
     */
    protected <T extends Object> T createObject(Class<T> clazz) throws Exception
    {
        return createObject(clazz, ClassUtils.NO_ARGS);
    }

    /**
     * Create an object of instance <code>clazz</code>. It will then register the object with the registry so that any
     * dependencies are injected and then the object will be initialised.
     * Note that if the object needs to be configured with additional state that cannot be passed into the constructor you should
     * create an instance first set any additional data on the object then call {@link #initialiseObject(Object)}.
     *
     * @param clazz the class to create an instance of.
     * @param args  constructor parameters
     * @param <T>   Object of this type will be returned
     * @return an initialised instance of <code>class</code>
     * @throws Exception if there is a problem creating or initializing the object
     */
    @SuppressWarnings("unchecked")
    protected <T extends Object> T createObject(Class<T> clazz, Object... args) throws Exception
    {
        if (args == null)
        {
            args = ClassUtils.NO_ARGS;
        }
        Object o = ClassUtils.instanciateClass(clazz, args);
        muleContext.getRegistry().registerObject(String.valueOf(o.hashCode()), o);
        return (T) o;
    }

    /**
     * A convenience method that will register an object in the registry using its hashcode as the key.  This will cause the object
     * to have any objects injected and lifecycle methods called.  Note that the object lifecycle will be called to the same current
     * lifecycle as the MuleContext
     *
     * @param o the object to register and initialise it
     * @throws org.mule.api.registry.RegistrationException
     */
    protected void initialiseObject(Object o) throws RegistrationException
    {
        muleContext.getRegistry().registerObject(String.valueOf(o.hashCode()), o);
    }

    public SensingNullMessageProcessor getSensingNullMessageProcessor()
    {
        return new SensingNullMessageProcessor();
    }

    public TriggerableMessageSource getTriggerableMessageSource(MessageProcessor listener)
    {
        return new TriggerableMessageSource(listener);
    }

    public TriggerableMessageSource getTriggerableMessageSource()
    {
        return new TriggerableMessageSource();
    }

    /**
     * @return the mule application working directory where the app data is stored
     */
    protected File getWorkingDirectory()
    {
        return workingDirectory.getRoot();
    }

    /**
     * @param fileName name of the file. Can contain subfolders separated by {@link java.io.File#separator}
     * @return a File inside the working directory of the application.
     */
    protected File getFileInsideWorkingDirectory(String fileName)
    {
        return new File(getWorkingDirectory(), fileName);
    }
}