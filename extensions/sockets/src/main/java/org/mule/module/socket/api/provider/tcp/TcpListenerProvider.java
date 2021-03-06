/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.provider.tcp;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.module.socket.api.ConnectionSettings;
import org.mule.module.socket.api.connection.tcp.TcpListenerConnection;
import org.mule.module.socket.api.connection.tcp.protocol.SafeProtocol;
import org.mule.module.socket.api.socket.tcp.TcpProtocol;
import org.mule.module.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.module.socket.api.source.SocketListener;
import org.mule.module.socket.internal.SocketUtils;
import org.mule.module.socket.api.socket.factory.SimpleServerSocketFactory;
import org.mule.module.socket.api.socket.factory.SslServerSocketFactory;
import org.mule.module.socket.api.socket.factory.TcpServerSocketFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocket;


/**
 * A {@link ConnectionProvider} which provides instances of
 * {@link TcpListenerConnection} to be used by {@link SocketListener}
 *
 * @since 4.0
 */
@Alias("tcp-listener")
public final class TcpListenerProvider implements ConnectionProvider<TcpListenerConnection>, Initialisable
{

    /**
     * Its presence will imply the use of {@link SSLServerSocket}
     * instead of plain TCP {@link ServerSocket} for accepting new SSL connections.
     */
    @Parameter
    @Optional
    private TlsContextFactory tlsContext;

    /**
     * This configuration parameter refers to the address where the TCP socket should listen for incoming connections.
     */
    @ParameterGroup
    private ConnectionSettings connectionSettings;

    /**
     * {@link ServerSocket} configuration properties
     */
    @ParameterGroup
    private TcpServerSocketProperties tcpServerSocketProperties;

    /**
     * {@link TcpProtocol} that knows how the data is going to be read and written.
     * If not specified, the {@link SafeProtocol} will be used.
     */
    @Parameter
    @Optional
    private TcpProtocol protocol = new SafeProtocol();

    @Override
    public TcpListenerConnection connect() throws ConnectionException
    {
        SimpleServerSocketFactory serverSocketFactory = null;

        try
        {
            serverSocketFactory = tlsContext != null
                                  ? new SslServerSocketFactory(tlsContext)
                                  : new TcpServerSocketFactory();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }

        TcpListenerConnection connection = new TcpListenerConnection(connectionSettings, protocol,
                                                                     tcpServerSocketProperties,
                                                                     serverSocketFactory);
        connection.connect();
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect(TcpListenerConnection connection)
    {
        connection.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate(TcpListenerConnection connection)
    {
        return SocketUtils.validate(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<TcpListenerConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<TcpListenerConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.none();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (tlsContext != null && !tlsContext.isKeyStoreConfigured())
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("KeyStore must be configured for server side SSL"), this);
        }

        initialiseIfNeeded(tlsContext);
    }
}
