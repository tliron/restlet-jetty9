/**
 * Copyright 2014-2015 Three Crickets LLC and Restlet S.A.S.
 * <p>
 * The contents of this file are subject to the terms of the Apache 2.0 license:
 * http://www.opensource.org/licenses/apache-2.0
 * <p>
 * This code is a derivative of code that is copyright 2005-2014 Restlet S.A.S.,
 * available at: https://github.com/restlet/restlet-framework-java
 * <p>
 * Restlet is a registered trademark of Restlet S.A.S.
 */

package org.restlet.ext.jetty9;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Executor;

import javax.servlet.ServletException;

import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.engine.adapter.HttpServerHelper;
import org.restlet.ext.jetty9.internal.JettyServerCall;

/**
 * Abstract Jetty 9 Web server connector. Here is the list of parameters that
 * are supported. They should be set in the Server's context before it is
 * started:
 * <table summary="parameters">
 * <tr>
 * <th>Parameter name</th>
 * <th>Value type</th>
 * <th>Default value</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>connector.acceptorPriorityDelta</td>
 * <td>int</td>
 * <td>-2</td>
 * <td>Set the acceptor thread priority delta</td>
 * </tr>
 * <tr>
 * <td>connector.acceptors</td>
 * <td>int</td>
 * <td>-1</td>
 * <td>Connector acceptor thread count; when &lt; 0, Jetty will default to
 * {@link Runtime#availableProcessors()} / 8, with a minimum of 4; when 0, Jetty
 * will use the selector threads instead</td>
 * </tr>
 * <tr>
 * <td>connector.acceptQueueSize</td>
 * <td>int</td>
 * <td>0</td>
 * <td>Connector accept queue size; also known as accept backlog</td>
 * </tr>
 * <tr>
 * <td>connector.idleTimeout</td>
 * <td>int</td>
 * <td>30000</td>
 * <td>Connector idle timeout in milliseconds; see
 * {@link Socket#setSoTimeout(int)}; this value is interpreted as the maximum
 * time between some progress being made on the connection; so if a single byte
 * is read or written, then the timeout is reset</td>
 * </tr>
 * <tr>
 * <td>connector.inheritChannel</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>Sets whether this connector uses a channel inherited from the JVM</td>
 * </tr>
 * <tr>
 * <td>connector.reuseAddress</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>Whether the server socket reuses addresses</td>
 * </tr>
 * <tr>
 * <td>connector.selectors</td>
 * <td>int</td>
 * <td>-1</td>
 * <td>Connector selector thread count; when &lt;= 0, Jetty will default to
 * {@link Runtime#availableProcessors()} / 2, with a minimum of 4.</td>
 * </tr>
 * <tr>
 * <td>connector.soLingerTime</td>
 * <td>int</td>
 * <td>-1</td>
 * <td>Connector TCP/IP SO linger time in milliseconds; when -1 is disabled; see
 * {@link Socket#setSoLinger(boolean, int)}</td>
 * </tr>
 * <tr>
 * <td>connector.stopTimeout</td>
 * <td>long</td>
 * <td>30000</td>
 * <td>Connector stop timeout in milliseconds; the maximum time allowed for the
 * service to shutdown</td>
 * </tr>
 * <tr>
 * <td>ensureHostHeader</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>Whether to generate a Host header if not provided by the request</td>
 * </tr>
 * <tr>
 * <td>http.2</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>Whether to support HTTP/2</td>
 * </tr>
 * <tr>
 * <td>http.2c</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>Whether to support HTTP/2 cleartext (unencrypted)</td>
 * </tr>
 * <tr>
 * <td>http.delayDispatchUntilContent</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>If true, delay the application dispatch until content is available</td>
 * </tr>
 * <tr>
 * <td>http.headerCacheSize</td>
 * <td>int</td>
 * <td>512</td>
 * <td>HTTP header cache size in bytes</td>
 * </tr>
 * <tr>
 * <td>http.requestHeaderSize</td>
 * <td>int</td>
 * <td>8*1024</td>
 * <td>HTTP request header size in bytes; larger headers will allow for more
 * and/or larger cookies plus larger form content encoded in a URL; however,
 * larger headers consume more memory and can make a server more vulnerable to
 * denial of service attacks</td>
 * </tr>
 * <tr>
 * <td>http.responseHeaderSize</td>
 * <td>int</td>
 * <td>8*1024</td>
 * <td>HTTP response header size in bytes; larger headers will allow for more
 * and/or larger cookies and longer HTTP headers (e.g. for redirection);
 * however, larger headers will also consume more memory</td>
 * </tr>
 * <tr>
 * <td>http.outputAggregationSize</td>
 * <td>int</td>
 * <td>32*1024/4</td>
 * <td>Set the max size of the response content write that is copied into the
 * aggregate buffer</td>
 * </tr>
 * <tr>
 * <td>http.outputBufferSize</td>
 * <td>int</td>
 * <td>32*1024</td>
 * <td>HTTP output buffer size in bytes; a larger buffer can improve performance
 * by allowing a content producer to run without blocking, however larger
 * buffers consume more memory and may induce some latency before a client
 * starts processing the content</td>
 * </tr>
 * <tr>
 * <td>http.persistentConnectionsEnabled</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>True if HTTP/1 persistent connection are enabled</td>
 * </tr>
 * <tr>
 * <td>http.sendDateHeader</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>If true, include the Date in HTTP headers</td>
 * </tr>
 * <tr>
 * <td>http.sendVersionHeader</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>If true, send the Server header in responses</td>
 * </tr>
 * <tr>
 * <td>http.sendXPoweredBy</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>If true, send the X-Powered-By header in responses</td>
 * </tr>
 * <tr>
 * <td>lowResource.idleTimeout</td>
 * <td>int</td>
 * <td>1000</td>
 * <td>Low resource monitor idle timeout in milliseconds; applied to EndPoints
 * when in the low resources state</td>
 * </tr>
 * <tr>
 * <td>lowResource.maxConnections</td>
 * <td>int</td>
 * <td>0</td>
 * <td>Low resource monitor max connections; when 0, the check is disabled</td>
 * </tr>
 * <tr>
 * <td>lowResource.maxMemory</td>
 * <td>int</td>
 * <td>0</td>
 * <td>Low resource monitor max memory in bytes; when 0, the check is disabled;
 * memory used is calculated as (totalMemory-freeMemory)</td>
 * </tr>
 * <tr>
 * <td>lowResource.maxTime</td>
 * <td>int</td>
 * <td>0</td>
 * <td>The time in milliseconds that a low resource state can persist before the
 * low resource idle timeout is reapplied to all connections</td>
 * </tr>
 * <tr>
 * <td>lowResource.period</td>
 * <td>int</td>
 * <td>1000</td>
 * <td>Low resource monitor period in milliseconds; when 0, low resource
 * monitoring is disabled</td>
 * </tr>
 * <tr>
 * <td>lowResource.stopTimeout</td>
 * <td>long</td>
 * <td>30000</td>
 * <td>Low resource monitor stop timeout in milliseconds; the maximum time
 * allowed for the service to shutdown</td>
 * </tr>
 * <tr>
 * <td>lowResource.threads</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>Low resource monitor, whether to check if we're low on threads</td>
 * </tr>
 * <tr>
 * <td>threadPool.idleTimeout</td>
 * <td>int</td>
 * <td>60000</td>
 * <td>Thread pool idle timeout in milliseconds; threads that are idle for
 * longer than this period may be stopped</td>
 * </tr>
 * <tr>
 * <td>threadPool.maxThreads</td>
 * <td>int</td>
 * <td>200</td>
 * <td>Thread pool max threads</td>
 * </tr>
 * <tr>
 * <td>threadPool.minThreads</td>
 * <td>int</td>
 * <td>8</td>
 * <td>Thread pool minimum threads</td>
 * </tr>
 * <tr>
 * <td>threadPool.stopTimeout</td>
 * <td>long</td>
 * <td>5000</td>
 * <td>Thread pool stop timeout in milliseconds; the maximum time allowed for
 * the service to shutdown</td>
 * </tr>
 * <tr>
 * <td>threadPool.threadsPriority</td>
 * <td>int</td>
 * <td>{@link Thread#NORM_PRIORITY}</td>
 * <td>Thread pool threads priority</td>
 * </tr>
 * </table>
 * 
 * @see <a href="http://www.eclipse.org/jetty/">Jetty home page</a>
 * @author Jerome Louvel
 * @author Tal Liron
 */
public abstract class JettyServerHelper extends HttpServerHelper
{
	/**
	 * Constructor.
	 * 
	 * @param server
	 *        The server to help.
	 */
	public JettyServerHelper( Server server )
	{
		super( server );
	}

	@Override
	public void start() throws Exception
	{
		super.start();

		final org.eclipse.jetty.server.Server server = getWrappedServer();
		final ServerConnector connector = (ServerConnector) server.getConnectors()[0];

		getLogger().info( "Starting a Jetty HTTP/HTTPS server" );

		server.start();

		// We won't know the local port until after the server starts
		setEphemeralPort( connector.getLocalPort() );
	}

	@Override
	public void stop() throws Exception
	{
		getLogger().info( "Stopping a Jetty HTTP/HTTPS server" );

		getWrappedServer().stop();

		super.stop();
	}

	/**
	 * Set the acceptor thread priority delta.
	 * <p>
	 * This allows the acceptor thread to run at a different priority. Typically
	 * this would be used to lower the priority to give preference to handling
	 * previously accepted connections rather than accepting new connections.
	 * <p>
	 * Defaults to -2.
	 * 
	 * @return Connector acceptor priority delta.
	 */
	public int getConnectorAcceptorPriorityDelta()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.acceptorPriorityDelta", "-2" ) );
	}

	/**
	 * Connector acceptor thread count. Defaults to -1. When &lt; 0, Jetty will
	 * default to {@link Runtime#availableProcessors()} / 8, with a minimum of
	 * 4. When 0, Jetty will use the selector threads instead.
	 * 
	 * @return Connector acceptor thread count.
	 */
	public int getConnectorAcceptors()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.acceptors", "-1" ) );
	}

	/**
	 * Connector accept queue size. Defaults to 0.
	 * <p>
	 * Also known as accept backlog.
	 * 
	 * @return Connector accept queue size.
	 */
	public int getConnectorAcceptQueueSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.acceptQueueSize", "0" ) );
	}

	/**
	 * Connector byte buffer pool. Defaults to null. When null, will use a new
	 * {@link ArrayByteBufferPool}.
	 * 
	 * @return Connector byte buffer pool or null.
	 */
	public ByteBufferPool getConnectorByteBufferPool()
	{
		return null;
	}

	/**
	 * Connector executor. Defaults to null. When null, will use the server's
	 * thread pool.
	 * 
	 * @return Connector executor or null.
	 */
	public Executor getConnectorExecutor()
	{
		return null;
	}

	/**
	 * Connector idle timeout in milliseconds. Defaults to 30000.
	 * <p>
	 * See {@link Socket#setSoTimeout(int)}.
	 * <p>
	 * This value is interpreted as the maximum time between some progress being
	 * made on the connection. So if a single byte is read or written, then the
	 * timeout is reset.
	 * 
	 * @return Connector idle timeout.
	 */
	public int getConnectorIdleTimeout()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.idleTimeout", "30000" ) );
	}

	/**
	 * Sets whether this connector uses a channel inherited from the JVM.
	 * <p>
	 * If true, the connector first tries to inherit from a channel provided by
	 * the system. If there is no inherited channel available, or if the
	 * inherited channel is not usable, then it will fall back using
	 * {@link ServerSocketChannel}.
	 * <p>
	 * Use it with xinetd/inetd, to launch an instance of Jetty on demand. The
	 * port used to access pages on the Jetty instance is the same as the port
	 * used to launch Jetty.
	 * <p>
	 * Defaults to false.
	 * 
	 * @return Connector inherit channel.
	 */
	public boolean getConnectorInheritChannel()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "lowResource.inheritChannel", "false" ) );
	}

	/**
	 * Whether the server socket reuses addresses. Defaults to true.
	 * 
	 * @return Connector reuses addresses.
	 */
	public boolean getConnectorReuseAddress()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "lowResource.reuseAddress", "true" ) );
	}

	/**
	 * Connector scheduler. Defaults to null. When null, will use a new
	 * {@link ScheduledExecutorScheduler}.
	 * 
	 * @return Connector scheduler or null.
	 */
	public Scheduler getConnectorScheduler()
	{
		return null;
	}

	/**
	 * Connector selector thread count. Defaults to -1. When &lt;= 0, Jetty will
	 * default to {@link Runtime#availableProcessors()} / 2, with a minimum of
	 * 4.
	 * 
	 * @return Connector selector thread count.
	 */
	public int getConnectorSelectors()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.selectors", "-1" ) );
	}

	/**
	 * Connector TCP/IP SO linger time in milliseconds. Defaults to -1
	 * (disabled).
	 * <p>
	 * See {@link Socket#setSoLinger(boolean, int)}.
	 * 
	 * @return Connector TCP/IP SO linger time.
	 */
	public int getConnectorSoLingerTime()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.soLingerTime", "-1" ) );
	}

	/**
	 * Connector stop timeout in milliseconds. Defaults to 30000.
	 * <p>
	 * The maximum time allowed for the service to shutdown.
	 * 
	 * @return Connector stop timeout.
	 */
	public int getConnectorStopTimeout()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "connector.stopTimeout", "30000" ) );
	}

	/**
	 * Whether to generate a Host header if not provided by the request.
	 * Defaults to true.
	 * 
	 * @return Whether to generate a Host header
	 */
	public boolean getEnsureHostHeader()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "ensureHostHeader", "true" ) );
	}

	/**
	 * Whether to support HTTP/2. Defaults to false.
	 * 
	 * @return HTTP/2 support.
	 */
	public boolean getHttp2()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.2", "false" ) );
	}

	/**
	 * Whether to support HTTP/2 cleartext (unencrypted). Defaults to false.
	 * 
	 * @return HTTP/2 cleartext support.
	 */
	public boolean getHttp2c()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.2c", "false" ) );
	}

	/**
	 * If true, delay the application dispatch until content is available.
	 * Defaults to true.
	 * 
	 * @return HTTP delay dispatch until content.
	 */
	public boolean getHttpDelayDispatchUntilContent()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.delayDispatchUntilContent", "true" ) );
	}

	/**
	 * HTTP header cache size in bytes. Defaults to 512.
	 * 
	 * @return HTTP header cache size.
	 */
	public int getHttpHeaderCacheSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "http.headerCacheSize", "512" ) );
	}

	/**
	 * HTTP request header size in bytes. Defaults to 8*1024.
	 * <p>
	 * Larger headers will allow for more and/or larger cookies plus larger form
	 * content encoded in a URL. However, larger headers consume more memory and
	 * can make a server more vulnerable to denial of service attacks.
	 * 
	 * @return HTTP request header size.
	 */
	public int getHttpRequestHeaderSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "http.requestHeaderSize", "8192" ) );
	}

	/**
	 * HTTP response header size in bytes. Defaults to 8*1024.
	 * <p>
	 * Larger headers will allow for more and/or larger cookies and longer HTTP
	 * headers (e.g. for redirection). However, larger headers will also consume
	 * more memory.
	 * 
	 * @return HTTP response header size.
	 */
	public int getHttpResponseHeaderSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "http.responseHeaderSize", "8192" ) );
	}

	/**
	 * Set the max size of the response content write that is copied into the
	 * aggregate buffer. Writes that are smaller of this size are copied into
	 * the aggregate buffer, while writes that are larger of this size will
	 * cause the aggregate buffer to be flushed and the write to be executed
	 * without being copied. Defaults to 32*1024/4.
	 * 
	 * @return HTTP output aggregation size.
	 */
	public int getHttpOutputAggregationSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "http.outputAggregationSize", "8192" ) );
	}

	/**
	 * HTTP output buffer size in bytes. Defaults to 32*1024.
	 * <p>
	 * A larger buffer can improve performance by allowing a content producer to
	 * run without blocking, however larger buffers consume more memory and may
	 * induce some latency before a client starts processing the content.
	 * 
	 * @return HTTP output buffer size.
	 */
	public int getHttpOutputBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "http.outputBufferSize", "32768" ) );
	}

	/**
	 * True if HTTP/1 persistent connection are enabled. Defaults to true.
	 * 
	 * @return HTTP persistent connections enabled.
	 */
	public boolean getHttpPersistentConnectionsEnabled()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.persistentConnectionsEnabled", "true" ) );
	}

	/**
	 * If true, include the Date in HTTP headers. Defaults to true.
	 * 
	 * @return HTTP send date header.
	 */
	public boolean getHttpSendDateHeader()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.sendDateHeader", "true" ) );
	}

	/**
	 * If true, send the Server header in responses. Defaults to true.
	 * 
	 * @return HTTP send version header.
	 */
	public boolean getHttpSendVersionHeader()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.sendVersionHeader", "true" ) );
	}

	/**
	 * If true, send the X-Powered-By header in responses. Defaults to false.
	 * 
	 * @return HTTP send X-Powered-By
	 */
	public boolean getHttpSendXPoweredBy()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "http.sendXPoweredBy", "false" ) );
	}

	/**
	 * Low resource monitor idle timeout in milliseconds. Defaults to 1000.
	 * <p>
	 * Applied to EndPoints when in the low resources state.
	 * 
	 * @return Low resource monitor idle timeout.
	 */
	public int getLowResourceMonitorIdleTimeout()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "lowResource.idleTimeout", "1000" ) );
	}

	/**
	 * Low resource monitor max connections. Defaults to 0. When 0, the check is
	 * disabled.
	 * 
	 * @return Low resource monitor max connections.
	 */
	public int getLowResourceMonitorMaxConnections()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "lowResource.maxConnections", "0" ) );
	}

	/**
	 * Low resource monitor max memory in bytes. Defaults to 0. When 0, the
	 * check is disabled.
	 * <p>
	 * Memory used is calculated as (totalMemory-freeMemory).
	 * 
	 * @return Low resource monitor max memory.
	 */
	public long getLowResourceMonitorMaxMemory()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "lowResource.maxMemory", "0" ) );
	}

	/**
	 * The time in milliseconds that a low resource state can persist before the
	 * low resource idle timeout is reapplied to all connections. When 0, the
	 * low resource state can persist forever. Defaults to 0.
	 * 
	 * @return Low resource monitor max time.
	 */
	public int getLowResourceMonitorMaxTime()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "lowResource.maxTime", "0" ) );
	}

	/**
	 * Low resource monitor period in milliseconds. Defaults to 1000. When 0,
	 * low resource monitoring is disabled.
	 * 
	 * @return Low resource monitor period.
	 */
	public int getLowResourceMonitorPeriod()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "lowResource.period", "1000" ) );
	}

	/**
	 * Low resource monitor stop timeout in milliseconds. Defaults to 30000.
	 * <p>
	 * The maximum time allowed for the service to shutdown.
	 * 
	 * @return Low resource monitor stop timeout.
	 */
	public long getLowResourceMonitorStopTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "lowResource.stopTimeout", "30000" ) );
	}

	/**
	 * Low resource monitor, whether to check if we're low on threads. Defaults
	 * to true.
	 * 
	 * @return Low resource monitor threads.
	 */
	public boolean getLowResourceMonitorThreads()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "lowResource.threads", "true" ) );
	}

	/**
	 * Thread pool idle timeout in milliseconds. Defaults to 60000.
	 * <p>
	 * Threads that are idle for longer than this period may be stopped.
	 * 
	 * @return Thread pool idle timeout.
	 */
	public int getThreadPoolIdleTimeout()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "threadPool.idleTimeout", "60000" ) );
	}

	/**
	 * Thread pool maximum threads. Defaults to 200.
	 * 
	 * @return Thread pool maximum threads.
	 */
	public int getThreadPoolMaxThreads()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "threadPool.maxThreads", "200" ) );
	}

	/**
	 * Thread pool minimum threads. Defaults to 8.
	 * 
	 * @return Thread pool minimum threads.
	 */
	public int getThreadPoolMinThreads()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "threadPool.minThreads", "8" ) );
	}

	/**
	 * Thread pool stop timeout in milliseconds. Defaults to 5000.
	 * <p>
	 * The maximum time allowed for the service to shutdown.
	 * 
	 * @return Thread pool stop timeout.
	 */
	public long getThreadPoolStopTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "threadPool.stopTimeout", "5000" ) );
	}

	/**
	 * Thread pool threads priority. Defaults to {@link Thread#NORM_PRIORITY}.
	 * 
	 * @return Thread pool maximum threads.
	 */
	public int getThreadPoolThreadsPriority()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "threadPool.threadsPriority", String.valueOf( Thread.NORM_PRIORITY ) ) );
	}

	/**
	 * Creates new internal Jetty connection factories.
	 * 
	 * @param configuration
	 *        The HTTP configuration.
	 * @return New internal Jetty connection factories.
	 */
	protected ConnectionFactory[] createConnectionFactories( HttpConfiguration configuration )
	{
		final LinkedList<ConnectionFactory> connectionFactories = new LinkedList<ConnectionFactory>();

		NegotiatingServerConnectionFactory negotiator = null;

		boolean h2 = getHttp2();
		boolean h2c = getHttp2c();
		boolean legacy = false;

		for( Protocol protocol : getHelped().getProtocols() )
		{
			if( protocol.getName().equals( Http2.HTTPS_PROTOCOL.getName() ) && protocol.getVersion().equals( Http2.HTTPS_PROTOCOL.getVersion() ) )
				h2 = true;
			else if( protocol.getName().equals( Http2.HTTP_PROTOCOL.getName() ) && protocol.getVersion().equals( Http2.HTTP_PROTOCOL.getVersion() ) )
				h2c = true;
			else if( protocol.getName().equals( Protocol.HTTP.getName() ) && protocol.getVersion().equals( Protocol.HTTP.getVersion() ) )
				legacy = true;
			else if( protocol.getName().equals( Protocol.HTTPS.getName() ) && protocol.getVersion().equals( Protocol.HTTPS.getVersion() ) )
				legacy = true;
		}

		if( h2 )
		{
			// This will throw an exception if protocol negotiation is not
			// available
			NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();

			// ALPN negotiator
			negotiator = createDynamically( "org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory", "" );
		}

		// HTTP/2
		if( h2 )
			connectionFactories.add( createDynamically( "org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory", configuration ) );

		// HTTP/1.1
		if( legacy )
			connectionFactories.add( new HttpConnectionFactory( configuration ) );

		// HTTP/2 cleartext
		if( h2c )
			// Must be *after* legacy
			connectionFactories.add( createDynamically( "org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory", configuration ) );

		if( negotiator != null )
		{
			// Negotiate all protocols in order
			for( ConnectionFactory connectionFactory : connectionFactories )
				for( String protocol : connectionFactory.getProtocols() )
					negotiator.getNegotiatedProtocols().add( protocol.toLowerCase() );

			// Default to HTTP/1.1
			negotiator.setDefaultProtocol( connectionFactories.getLast().getProtocol().toLowerCase() );

			// The negotiator should be the first factory
			connectionFactories.add( 0, negotiator );
		}

		return connectionFactories.toArray( new ConnectionFactory[connectionFactories.size()] );
	}

	/**
	 * Returns the wrapped Jetty server.
	 * 
	 * @return The wrapped Jetty server.
	 */
	protected org.eclipse.jetty.server.Server getWrappedServer()
	{
		if( wrappedServer == null )
			wrappedServer = createServer();
		return wrappedServer;
	}

	/**
	 * Sets the wrapped Jetty server.
	 * 
	 * @param wrappedServer
	 *        The wrapped Jetty server.
	 */
	protected void setWrappedServer( org.eclipse.jetty.server.Server wrappedServer )
	{
		this.wrappedServer = wrappedServer;
	}

	/**
	 * Creates a Jetty HTTP configuration.
	 * 
	 * @return A Jetty HTTP configuration.
	 */
	private HttpConfiguration createConfiguration()
	{
		final HttpConfiguration configuration = new HttpConfiguration();
		configuration.setHeaderCacheSize( getHttpHeaderCacheSize() );
		configuration.setRequestHeaderSize( getHttpRequestHeaderSize() );
		configuration.setResponseHeaderSize( getHttpResponseHeaderSize() );
		configuration.setOutputBufferSize( getHttpOutputBufferSize() );
		configuration.setDelayDispatchUntilContent( getHttpDelayDispatchUntilContent() );
		configuration.setOutputAggregationSize( getHttpOutputAggregationSize() );
		configuration.setPersistentConnectionsEnabled( getHttpPersistentConnectionsEnabled() );
		configuration.setSendDateHeader( getHttpSendDateHeader() );
		configuration.setSendServerVersion( getHttpSendVersionHeader() );
		configuration.setSendXPoweredBy( getHttpSendXPoweredBy() );
		return configuration;
	}

	/**
	 * Creates a Jetty server.
	 * 
	 * @return A Jetty server.
	 */
	private org.eclipse.jetty.server.Server createServer()
	{
		// Thread pool
		final ThreadPool threadPool = createThreadPool();

		// Server
		final org.eclipse.jetty.server.Server server = new WrappedServer( this, threadPool );

		// Connector
		final Connector connector = createConnector( server );
		server.addConnector( connector );

		// Low resource monitor (must be created after connectors have been
		// added)
		createLowResourceMonitor( server );

		return server;
	}

	/**
	 * Creates a Jetty thread pool.
	 * 
	 * @return A Jetty thread pool.
	 */
	private ThreadPool createThreadPool()
	{
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMinThreads( getThreadPoolMinThreads() );
		threadPool.setMaxThreads( getThreadPoolMaxThreads() );
		threadPool.setThreadsPriority( getThreadPoolThreadsPriority() );
		threadPool.setIdleTimeout( getThreadPoolIdleTimeout() );
		threadPool.setStopTimeout( getThreadPoolStopTimeout() );
		// threadPool.setDaemon( daemon );
		// threadPool.setDetailedDump( detailedDump );
		return threadPool;
	}

	/**
	 * Creates a Jetty connector.
	 * 
	 * @param server
	 *        The Jetty server.
	 * @return A Jetty connector.
	 */
	private Connector createConnector( org.eclipse.jetty.server.Server server )
	{
		final HttpConfiguration configuration = createConfiguration();
		final ConnectionFactory[] connectionFactories = createConnectionFactories( configuration );

		final int acceptors = getConnectorAcceptors();
		final int selectors = getConnectorSelectors();
		final Executor executor = getConnectorExecutor();
		final Scheduler scheduler = getConnectorScheduler();
		final ByteBufferPool byteBufferPool = getConnectorByteBufferPool();

		final ServerConnector connector = new ServerConnector( server, executor, scheduler, byteBufferPool, acceptors, selectors, connectionFactories );

		final String address = getHelped().getAddress();
		if( address != null )
			connector.setHost( address );
		connector.setPort( getHelped().getPort() );

		connector.setAcceptQueueSize( getConnectorAcceptQueueSize() );
		connector.setIdleTimeout( getConnectorIdleTimeout() );
		connector.setSoLingerTime( getConnectorSoLingerTime() );
		connector.setStopTimeout( getConnectorStopTimeout() );
		connector.setReuseAddress( getConnectorReuseAddress() );
		connector.setAcceptorPriorityDelta( getConnectorAcceptorPriorityDelta() );
		connector.setInheritChannel( getConnectorInheritChannel() );

		return connector;
	}

	/**
	 * Creates a Jetty low resource monitor.
	 * 
	 * @param server
	 *        A Jetty server.
	 * @return A Jetty low resource monitor or null.
	 */
	private LowResourceMonitor createLowResourceMonitor( org.eclipse.jetty.server.Server server )
	{
		final int period = getLowResourceMonitorPeriod();
		if( period > 0 )
		{
			final LowResourceMonitor lowResourceMonitor = new LowResourceMonitor( server );
			lowResourceMonitor.setMonitoredConnectors( Arrays.asList( server.getConnectors() ) );
			lowResourceMonitor.setPeriod( period );
			lowResourceMonitor.setMonitorThreads( getLowResourceMonitorThreads() );
			lowResourceMonitor.setMaxConnections( getLowResourceMonitorMaxConnections() );
			lowResourceMonitor.setMaxMemory( getLowResourceMonitorMaxMemory() );
			lowResourceMonitor.setMaxLowResourcesTime( getLowResourceMonitorMaxTime() );
			lowResourceMonitor.setLowResourcesIdleTimeout( getLowResourceMonitorIdleTimeout() );
			lowResourceMonitor.setStopTimeout( getLowResourceMonitorStopTimeout() );
			server.addBean( lowResourceMonitor );
			return lowResourceMonitor;
		}
		return null;
	}

	/**
	 * Jetty server wrapped by a parent Restlet HTTP server connector.
	 * 
	 * @author Jerome Louvel
	 * @author Tal Liron
	 */
	private static class WrappedServer extends org.eclipse.jetty.server.Server
	{
		/**
		 * Constructor.
		 * 
		 * @param helper
		 *        The Jetty HTTP server.
		 * @param threadPool
		 *        The thread pool.
		 */
		public WrappedServer( JettyServerHelper helper, ThreadPool threadPool )
		{
			super( threadPool );
			this.helper = helper;
			ensureHostHeader = helper.getEnsureHostHeader();
		}

		/**
		 * Handler method converting a Jetty HttpChannel into a Restlet Call.
		 * 
		 * @param channel
		 *        The channel to handle.
		 */
		@Override
		public void handle( HttpChannel channel ) throws IOException, ServletException
		{
			try
			{
				helper.handle( new JettyServerCall( helper.getHelped(), channel, ensureHostHeader ) );
			}
			catch( Throwable e )
			{
				channel.getEndPoint().close();
				throw new IOException( "Restlet exception", e );
			}
		}

		@Override
		public void handleAsync( HttpChannel channel ) throws IOException, ServletException
		{
			// TODO: should we handle async differently?
			handle( channel );
		}

		private final JettyServerHelper helper;

		private final boolean ensureHostHeader;
	}

	/**
	 * Creates a class instance dynamically, via reflection.
	 * 
	 * @param className
	 *        The class name
	 * @param params
	 *        The optional params
	 * @return The new instance
	 */
	private static <T> T createDynamically( String className, Object... params )
	{
		try
		{
			@SuppressWarnings("unchecked")
			final Class<T> clazz = (Class<T>) Class.forName( className );
			final int length = params.length;
			final Class<?>[] classes = new Class<?>[length];
			for( int i = 0; i < length; i++ )
				classes[i] = params[i].getClass();
			Constructor<T> constructor = clazz.getConstructor( classes );
			return constructor.newInstance( params );
		}
		catch( ClassNotFoundException x )
		{
			throw new NoClassDefFoundError( className );
		}
		catch( NoSuchMethodException x )
		{
			throw new RuntimeException( x );
		}
		catch( SecurityException x )
		{
			throw new RuntimeException( x );
		}
		catch( InstantiationException x )
		{
			throw new RuntimeException( x );
		}
		catch( IllegalAccessException x )
		{
			throw new RuntimeException( x );
		}
		catch( IllegalArgumentException x )
		{
			throw new RuntimeException( x );
		}
		catch( InvocationTargetException x )
		{
			throw new RuntimeException( x );
		}
	}

	/** The wrapped Jetty server. */
	private volatile org.eclipse.jetty.server.Server wrappedServer;
}
