/**
 * Copyright 2013 Three Crickets LLC. The contents of this file are subject to
 * the terms of the Apache 2.0 license:
 * http://www.opensource.org/licenses/apache-2.0
 * <p>
 * This code is a derivative of code that is copyright 2005-2013 Restlet S.A.S.
 * <p>
 * That code was taken from the Restlet repository:
 * https://github.com/restlet/restlet-framework-java
 * <p>
 * Restlet is a registered trademark of Restlet S.A.S.
 */

package org.restlet.ext.jetty9;

import java.io.IOException;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.restlet.ext.jetty9.internal.JettyCall;

/**
 * Abstract Jetty Web server connector. Here is the list of parameters that are
 * supported. They should be set in the Server's context before it is started:
 * <table>
 * <tr>
 * <th>Parameter name</th>
 * <th>Value type</th>
 * <th>Default value</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>minThreads</td>
 * <td>int</td>
 * <td>1</td>
 * <td>Minimum threads waiting to service requests.</td>
 * </tr>
 * <tr>
 * <td>maxThreads</td>
 * <td>int</td>
 * <td>255</td>
 * <td>Maximum threads that will service requests.</td>
 * </tr>
 * <tr>
 * <td>threadMaxIdleTimeMs</td>
 * <td>int</td>
 * <td>60000</td>
 * <td>Time for an idle thread to wait for a request or read.</td>
 * </tr>
 * <tr>
 * <td>lowResourcesMaxIdleTimeMs</td>
 * <td>int</td>
 * <td>2500</td>
 * <td>Time in ms that connections will persist if listener is low on resources.
 * </td>
 * </tr>
 * <tr>
 * <td>acceptorThreads</td>
 * <td>int</td>
 * <td>1</td>
 * <td>Number of acceptor threads to set.</td>
 * </tr>
 * <tr>
 * <td>acceptQueueSize</td>
 * <td>int</td>
 * <td>0</td>
 * <td>Size of the accept queue.</td>
 * </tr>
 * <tr>
 * <td>requestHeaderSize</td>
 * <td>int</td>
 * <td>4*1024</td>
 * <td>Size of the buffer to be used for request headers.</td>
 * </tr>
 * <tr>
 * <td>responseHeaderSize</td>
 * <td>int</td>
 * <td>4*1024</td>
 * <td>Size of the buffer to be used for response headers.</td>
 * </tr>
 * <tr>
 * <td>requestBufferSize</td>
 * <td>int</td>
 * <td>8*1024</td>
 * <td>Size of the content buffer for receiving requests.</td>
 * </tr>
 * <tr>
 * <td>responseBufferSize</td>
 * <td>int</td>
 * <td>32*1024</td>
 * <td>Size of the content buffer for sending responses.</td>
 * </tr>
 * <tr>
 * <td>ioMaxIdleTimeMs</td>
 * <td>int</td>
 * <td>30000</td>
 * <td>Maximum time to wait on an idle IO operation.</td>
 * </tr>
 * <tr>
 * <td>soLingerTime</td>
 * <td>int</td>
 * <td>1000</td>
 * <td>SO linger time (see Jetty documentation).</td>
 * </tr>
 * <tr>
 * <td>gracefulShutdown</td>
 * <td>int</td>
 * <td>0</td>
 * <td>The time (in ms) to wait for existing requests to complete before fully
 * stopping the server.</td>
 * </tr>
 * </table>
 * 
 * @see <a href="http://www.eclipse.org/jetty/">Jetty home page</a>
 * @author Jerome Louvel
 * @author Tal Liron
 */
public abstract class JettyServerHelper extends org.restlet.engine.adapter.HttpServerHelper
{
	/**
	 * Jetty server wrapped by a parent Restlet HTTP server connector.
	 * 
	 * @author Jerome Louvel
	 */
	private static class WrappedServer extends org.eclipse.jetty.server.Server
	{
		JettyServerHelper helper;

		/**
		 * Constructor.
		 * 
		 * @param server
		 *        The Jetty HTTP server.
		 * @param threadPool
		 *        The thread pool.
		 */
		public WrappedServer( JettyServerHelper server, ThreadPool threadPool )
		{
			super( threadPool );
			this.helper = server;
		}

		/**
		 * Handler method converting a Jetty HttpChannel into a Restlet Call.
		 * 
		 * @param channel
		 *        The channel to handle.
		 */
		@Override
		public void handle( HttpChannel<?> channel ) throws IOException, ServletException
		{
			this.helper.handle( new JettyCall( this.helper.getHelped(), channel ) );
		}
	}

	/** The wrapped Jetty server. */
	private volatile Server wrappedServer;

	/** The internal Jetty connector. */
	private volatile ServerConnector connector;

	/**
	 * Constructor.
	 * 
	 * @param server
	 *        The server to help.
	 */
	public JettyServerHelper( org.restlet.Server server )
	{
		super( server );
		this.connector = null;
		this.wrappedServer = null;
	}

	/**
	 * Configures the internal Jetty connector.
	 * 
	 * @param connector
	 *        The internal Jetty connector.
	 */
	protected void configure( ServerConnector connector )
	{
		if( getHelped().getAddress() != null )
		{
			connector.setHost( getHelped().getAddress() );
		}

		HttpConfiguration configuration = new HttpConfiguration();
		configuration.setResponseHeaderSize( getResponseHeaderSize() );
		configuration.setRequestHeaderSize( getRequestHeaderSize() );
		configuration.setOutputBufferSize( getResponseBufferSize() );

		connector.setPort( getHelped().getPort() );
		connector.setAcceptQueueSize( getAcceptQueueSize() );
		// connector.setRequestBufferSize( getRequestBufferSize() );
		connector.setIdleTimeout( getIoMaxIdleTimeMs() );
		connector.setSoLingerTime( getSoLingerTime() );

		final LowResourceMonitor lowResourceMonitor = new LowResourceMonitor( getWrappedServer() );
		lowResourceMonitor.setLowResourcesIdleTimeout( getLowResourcesMaxIdleTimeMs() );
		getWrappedServer().addBean( lowResourceMonitor );
	}

	/**
	 * Creates a new internal Jetty connector.
	 * 
	 * @return A new internal Jetty connector.
	 */
	protected abstract ServerConnector createConnector( Server server );

	/**
	 * Returns the number of acceptor threads to set.
	 * 
	 * @return The number of acceptor threads to set.
	 */
	public int getAcceptorThreads()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "acceptorThreads", "1" ) );
	}

	/**
	 * Returns the size of the accept queue.
	 * 
	 * @return The size of the accept queue.
	 */
	public int getAcceptQueueSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "acceptQueueSize", "0" ) );
	}

	/**
	 * Returns the time (in ms) to wait for existing requests to complete before
	 * fully stopping the server.
	 * 
	 * @return The graceful shutdown delay.
	 */
	public int getGracefulShutdown()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "gracefulShutdown", "0" ) );
	}

	/**
	 * Returns the maximum time to wait on an idle IO operation.
	 * 
	 * @return The maximum time to wait on an idle IO operation.
	 */
	public int getIoMaxIdleTimeMs()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "ioMaxIdleTimeMs", "30000" ) );
	}

	/**
	 * Returns the time in ms that connections will persist if listener is low
	 * on resources.
	 * 
	 * @return The time in ms that connections will persist if listener is low
	 *         on resources.
	 */
	public int getLowResourcesMaxIdleTimeMs()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "lowResourcesMaxIdleTimeMs", "2500" ) );
	}

	/**
	 * Returns the maximum threads that will service requests.
	 * 
	 * @return The maximum threads that will service requests.
	 */
	public int getMaxThreads()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxThreads", "255" ) );
	}

	/**
	 * Returns the minimum threads waiting to service requests.
	 * 
	 * @return The minimum threads waiting to service requests.
	 */
	public int getMinThreads()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "minThreads", "1" ) );
	}

	/**
	 * Returns the size of the content buffer for receiving requests.
	 * 
	 * @return The size of the content buffer for receiving requests.
	 */
	public int getRequestBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "requestBufferSize", Integer.toString( 8 * 1024 ) ) );
	}

	/**
	 * Returns the size of the buffer to be used for request headers.
	 * 
	 * @return The size of the buffer to be used for request headers.
	 */
	public int getRequestHeaderSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "requestHeaderSize", Integer.toString( 4 * 1024 ) ) );
	}

	/**
	 * Returns the size of the content buffer for sending responses.
	 * 
	 * @return The size of the content buffer for sending responses.
	 */
	public int getResponseBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "responseBufferSize", Integer.toString( 32 * 1024 ) ) );
	}

	/**
	 * Returns the size of the buffer to be used for response headers.
	 * 
	 * @return The size of the buffer to be used for response headers.
	 */
	public int getResponseHeaderSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "responseHeaderSize", Integer.toString( 4 * 1024 ) ) );
	}

	/**
	 * Returns the SO linger time (see Jetty 6 documentation).
	 * 
	 * @return The SO linger time (see Jetty 6 documentation).
	 */
	public int getSoLingerTime()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "soLingerTime", "1000" ) );
	}

	/**
	 * Returns the time for an idle thread to wait for a request or read.
	 * 
	 * @return The time for an idle thread to wait for a request or read.
	 */
	public int getThreadMaxIdleTimeMs()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "threadMaxIdleTimeMs", "60000" ) );
	}

	/**
	 * Returns the wrapped Jetty server.
	 * 
	 * @return The wrapped Jetty server.
	 */
	protected Server getWrappedServer()
	{
		if( this.wrappedServer == null )
		{
			// Configuring the thread pool
			QueuedThreadPool btp = new QueuedThreadPool();
			btp.setIdleTimeout( getThreadMaxIdleTimeMs() );
			btp.setMaxThreads( getMaxThreads() );
			btp.setMinThreads( getMinThreads() );

			this.wrappedServer = new WrappedServer( this, btp );

			if( getGracefulShutdown() > 0 )
			{
				// getWrappedServer().setGracefulShutdown( getGracefulShutdown()
				// );
			}
		}

		return this.wrappedServer;
	}

	/**
	 * Sets the wrapped Jetty server.
	 * 
	 * @param wrappedServer
	 *        The wrapped Jetty server.
	 */
	protected void setWrappedServer( Server wrappedServer )
	{
		this.wrappedServer = wrappedServer;
	}

	@Override
	public void start() throws Exception
	{
		if( this.connector == null )
		{
			this.connector = createConnector( getWrappedServer() );
			configure( this.connector );
			getWrappedServer().addConnector( this.connector );
		}

		getWrappedServer().start();
		setEphemeralPort( this.connector.getLocalPort() );
	}

	@Override
	public void stop() throws Exception
	{
		getWrappedServer().stop();
	}
}
