/**
 * Copyright 2014-2016 Three Crickets LLC and Restlet S.A.S.
 * <p>
 * The contents of this file are subject to the terms of the Jetty 2.0 license:
 * http://www.opensource.org/licenses/apache-2.0
 * <p>
 * This code is a derivative of code that is copyright 2005-2014 Restlet S.A.S.,
 * available at: https://github.com/restlet/restlet-framework-java
 * <p>
 * Restlet is a registered trademark of Restlet S.A.S.
 */

package org.restlet.ext.jetty9;

import java.io.IOException;
import java.net.CookieStore;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.data.Protocol;
import org.restlet.engine.adapter.ClientCall;
import org.restlet.engine.adapter.HttpClientHelper;
import org.restlet.engine.ssl.DefaultSslContextFactory;
import org.restlet.engine.util.ReferenceUtils;
import org.restlet.ext.jetty9.internal.JettyClientCall;

/**
 * HTTP client connector using the Jetty project.<br>
 * <br>
 * Here is the list of parameters that are supported. They should be set in the
 * Client's context before it is started:
 * <table summary="parameters">
 * <tr>
 * <th>Parameter name</th>
 * <th>Value type</th>
 * <th>Default value</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>addressResolutionTimeout</td>
 * <td>long</td>
 * <td>15000</td>
 * <td>The timeout in milliseconds for the DNS resolution of host addresses</td>
 * </tr>
 * <tr>
 * <td>bindAddress</td>
 * <td>String</td>
 * <td>null</td>
 * <td>The address to bind socket channels to. You must set <i>both</i> this and
 * bindPort</td>
 * </tr>
 * <tr>
 * <td>bindPort</td>
 * <td>int</td>
 * <td>null</td>
 * <td>The address to bind socket channels to. You must set <i>both</i> this and
 * bindAddress</td>
 * </tr>
 * <tr>
 * <td>connectTimeout</td>
 * <td>long</td>
 * <td>15000</td>
 * <td>The max time in milliseconds a connection can take to connect to
 * destinations</td>
 * </tr>
 * <tr>
 * <td>followRedirects</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>Whether to follow HTTP redirects</td>
 * </tr>
 * <tr>
 * <td>idleTimeout</td>
 * <td>long</td>
 * <td>0</td>
 * <td>The max time in milliseconds a connection can be idle (that is, without
 * traffic of bytes in either direction)</td>
 * </tr>
 * <tr>
 * <td>maxConnectionsPerDestination</td>
 * <td>int</td>
 * <td>64</td>
 * <td>Sets the max number of connections to open to each destination</td>
 * </tr>
 * <tr>
 * <td>maxRedirects</td>
 * <td>int</td>
 * <td>8</td>
 * <td>The max number of HTTP redirects that are followed</td>
 * </tr>
 * <tr>
 * <td>maxRequestsQueuedPerDestination</td>
 * <td>int</td>
 * <td>1024</td>
 * <td>Sets the max number of requests that may be queued to a destination</td>
 * </tr>
 * <tr>
 * <td>removeIdleDestinations</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>Whether destinations that have no connections (nor active nor idle)
 * should be removed</td>
 * </tr>
 * <tr>
 * <td>requestBufferSize</td>
 * <td>int</td>
 * <td>4096</td>
 * <td>The size in bytes of the buffer used to write requests</td>
 * </tr>
 * <tr>
 * <td>responseBufferSize</td>
 * <td>int</td>
 * <td>16384</td>
 * <td>The size in bytes of the buffer used to read responses</td>
 * </tr>
 * <tr>
 * <td>stopTimeout</td>
 * <td>long</td>
 * <td>30000</td>
 * <td>Stop timeout in milliseconds; the maximum time allowed for the service to
 * shutdown</td>
 * </tr>
 * <tr>
 * <td>strictEventOrdering</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>Whether request events must be strictly ordered</td>
 * </tr>
 * <tr>
 * <td>tcpNoDelay</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>Whether TCP_NODELAY is enabled</td>
 * </tr>
 * <tr>
 * <td>timeout</td>
 * <td>long</td>
 * <td>5000</td>
 * <td>Request timeout in milliseconds</td>
 * </tr>
 * <tr>
 * <td>userAgentField</td>
 * <td>String</td>
 * <td>null</td>
 * <td>The "User-Agent" HTTP header string; when null, uses the Jetty default
 * </td>
 * </tr>
 * <tr>
 * <td>sslContextFactory</td>
 * <td>String</td>
 * <td>org.restlet.ext.ssl.DefaultSslContextFactory</td>
 * <td>Let you specify a {@link SslContextFactory} qualified class name as a
 * parameter, or an instance as an attribute for a more complete and flexible
 * SSL context setting</td>
 * </tr>
 * </table>
 * For the default SSL parameters see the Javadocs of the
 * {@link DefaultSslContextFactory} class.
 * 
 * @see <a href="http://www.eclipse.org/jetty/">Jetty home page</a>
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class JettyHttpClientHelper extends HttpClientHelper
{
	/**
	 * Constructor.
	 * 
	 * @param client
	 *        The client to help.
	 */
	public JettyHttpClientHelper( Client client )
	{
		super( client );
		getProtocols().add( Protocol.HTTP );
		getProtocols().add( Protocol.HTTPS );
	}

	/**
	 * Creates a low-level HTTP client call from a high-level uniform call.
	 * 
	 * @param request
	 *        The high-level request.
	 * @return A low-level HTTP client call.
	 */
	public ClientCall create( Request request )
	{
		ClientCall result = null;

		try
		{
			result = new JettyClientCall( this, request.getMethod().toString(), ReferenceUtils.update( request.getResourceRef(), request ).toString() );
		}
		catch( IOException e )
		{
			getLogger().log( Level.WARNING, "Unable to create the Jetty HTTP/HTTPS client call", e );
		}

		return result;
	}

	/**
	 * Returns the wrapped Jetty HTTP client.
	 * 
	 * @return The wrapped Jetty HTTP client.
	 */
	public HttpClient getHttpClient()
	{
		return httpClient;
	}

	@Override
	public void start() throws Exception
	{
		super.start();

		if( httpClient == null )
			httpClient = createHttpClient();

		final HttpClient httpClient = getHttpClient();
		if( httpClient != null )
		{
			getLogger().info( "Starting a Jetty HTTP/HTTPS client" );
			httpClient.start();
		}
	}

	@Override
	public void stop() throws Exception
	{
		final HttpClient httpClient = getHttpClient();
		if( httpClient != null )
		{
			getLogger().info( "Stopping a Jetty HTTP/HTTPS client" );
			httpClient.stop();
		}

		super.stop();
	}

	/**
	 * The timeout in milliseconds for the DNS resolution of host addresses.
	 * Defaults to 15000.
	 * 
	 * @return The address resolution timeout.
	 */
	public long getAddressResolutionTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "addressResolutionTimeout", "15000" ) );
	}

	/**
	 * The address to bind socket channels to. Defaults to null.
	 * 
	 * @return The bind address or null.
	 */
	public SocketAddress getBindAddress()
	{
		final String bindAddress = getHelpedParameters().getFirstValue( "bindAddress", null );
		final String bindPort = getHelpedParameters().getFirstValue( "bindPort", null );
		if( ( bindAddress != null ) && ( bindPort != null ) )
			return new InetSocketAddress( bindAddress, Integer.parseInt( bindPort ) );
		return null;
	}

	/**
	 * The {@link ByteBufferPool} of this {@link HttpClient}. When null, uses a
	 * {@link MappedByteBufferPool}. Defaults to null.
	 * 
	 * @return The byte buffer pool or null.
	 */
	public ByteBufferPool getByteBufferPool()
	{
		return null;
	}

	/**
	 * The max time in milliseconds a connection can take to connect to
	 * destinations. Defaults to 15000.
	 * 
	 * @return The connect timeout.
	 */
	public long getConnectTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "connectTimeout", "15000" ) );
	}

	/**
	 * Whether to follow HTTP redirects. Defaults to true.
	 * 
	 * @return Whether to follow redirects.
	 */
	public boolean isFollowRedirects()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "followRedirects", "true" ) );
	}

	/**
	 * The max time in milliseconds a connection can be idle (that is, without
	 * traffic of bytes in either direction). Defaults to 0.
	 * 
	 * @return The idle timeout.
	 */
	public long getIdleTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "idleTimeout", "0" ) );
	}

	/**
	 * Sets the max number of connections to open to each destination. Defaults
	 * to 64.
	 * <p>
	 * RFC 2616 suggests that 2 connections should be opened per each
	 * destination, but browsers commonly open 6. If this client is used for
	 * load testing, it is common to have only one destination (the server to
	 * load test), and it is recommended to set this value to a high value (at
	 * least as much as the threads present in the {@link #getExecutor()
	 * executor}).
	 * 
	 * @return The maximum connections per destination.
	 */
	public int getMaxConnectionsPerDestination()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxConnectionsPerDestination", "64" ) );
	}

	/**
	 * The max number of HTTP redirects that are followed. Defaults to 8.
	 * 
	 * @return The maximum redirects.
	 */
	public int getMaxRedirects()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxRedirects", "8" ) );
	}

	/**
	 * Sets the max number of requests that may be queued to a destination.
	 * Defaults to 1024.
	 * <p>
	 * If this client performs a high rate of requests to a destination, and all
	 * the connections managed by that destination are busy with other requests,
	 * then new requests will be queued up in the destination. This parameter
	 * controls how many requests can be queued before starting to reject them.
	 * If this client is used for load testing, it is common to have this
	 * parameter set to a high value, although this may impact latency (requests
	 * sit in the queue for a long time before being sent).
	 * 
	 * @return The maximum requests queues per destination.
	 */
	public int getMaxRequestsQueuedPerDestination()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxRequestsQueuedPerDestination", "1024" ) );
	}

	/**
	 * Whether destinations that have no connections (nor active nor idle)
	 * should be removed.
	 * <p>
	 * Applications typically make request to a limited number of destinations
	 * so keeping destinations around is not a problem for the memory or the GC.
	 * However, for applications that hit millions of different destinations
	 * (e.g. a spider bot) it would be useful to be able to remove the old
	 * destinations that won't be visited anymore and leave space for new
	 * destinations.
	 *
	 * @return Whether destinations that have no connections should be removed.
	 */
	public boolean getRemoveIdleDestinations()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "removeIdleDestinations", "false" ) );
	}

	/**
	 * The size in bytes of the buffer used to write requests. Defaults to 4096.
	 * 
	 * @return The request buffer size.
	 */
	public int getRequestBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "requestBufferSize", "4096" ) );
	}

	/**
	 * The size in bytes of the buffer used to read responses. Defaults to
	 * 16384.
	 * 
	 * @return The response buffer size.
	 */
	public int getResponseBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "responseBufferSize", "16384" ) );
	}

	/**
	 * Stop timeout in milliseconds. Defaults to 30000.
	 * <p>
	 * The maximum time allowed for the service to shutdown.
	 * 
	 * @return The stop timeout.
	 */
	public long getStopTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "stopTimeout", "30000" ) );
	}

	/**
	 * Whether request events must be strictly ordered. Defaults to false.
	 * <p>
	 * Client listeners may send a second request. If the second request is for
	 * the same destination, there is an inherent race condition for the use of
	 * the connection: the first request may still be associated with the
	 * connection, so the second request cannot use that connection and is
	 * forced to open another one.
	 * <p>
	 * From the point of view of connection usage, the connection is reusable
	 * just before the "complete" event, so it would be possible to reuse that
	 * connection from complete listeners; but in this case the second request's
	 * events will fire before the "complete" events of the first request.
	 * <p>
	 * This setting enforces strict event ordering so that a "begin" event of a
	 * second request can never fire before the "complete" event of a first
	 * request, but at the expense of an increased usage of connections.
	 * <p>
	 * When not enforced, a "begin" event of a second request may happen before
	 * the "complete" event of a first request and allow for better usage of
	 * connections.
	 * 
	 * @return Whether request events must be strictly ordered.
	 */
	public boolean isStrictEventOrdering()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "strictEventOrdering", "false" ) );
	}

	/**
	 * Whether TCP_NODELAY is enabled. Defaults to true.
	 * 
	 * @return Whether TCP_NODELAY is enabled.
	 */
	public boolean isTcpNoDelay()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "tcpNoDelay", "true" ) );
	}

	/**
	 * Request timeout in milliseconds. Defaults to 5000.
	 * <p>
	 * The maximum time allowed for a request to receive a response.
	 * 
	 * @return The request timeout.
	 */
	public long getTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "timeout", "5000" ) );
	}

	/**
	 * The "User-Agent" HTTP header string. When null, uses the Jetty default.
	 * Defaults to null.
	 * 
	 * @return The user agent field or null.
	 */
	public String getUserAgentField()
	{
		return getHelpedParameters().getFirstValue( "userAgentField", null );
	}

	/**
	 * The cookie store. Defaults to null. When null, creates a new instance of
	 * sun.net.www.protocol.http.InMemoryCookieStore.
	 * 
	 * @return The cookie store.
	 */
	public CookieStore getCookieStore()
	{
		return null;
	}

	/**
	 * The executor. Defaults to null. When null, creates a new instance of
	 * {@link QueuedThreadPool}.
	 * 
	 * @return The executor.
	 */
	public Executor getExecutor()
	{
		return null;
	}

	/**
	 * The scheduler. Defaults to null. When null, creates a new instance of
	 * {@link ScheduledExecutorScheduler}.
	 * 
	 * @return The scheduler.
	 */
	public Scheduler getScheduler()
	{
		return null;
	}

	/**
	 * Creates a Jetty HTTP client.
	 * 
	 * @return A new HTTP client.
	 */
	private HttpClient createHttpClient()
	{
		SslContextFactory sslContextFactory = null;
		try
		{
			final org.restlet.engine.ssl.SslContextFactory restletSslContextFactory = org.restlet.engine.ssl.SslUtils.getSslContextFactory( this );
			sslContextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory();
			sslContextFactory.setSslContext( restletSslContextFactory.createSslContext() );
		}
		catch( Exception e )
		{
		}

		final HttpClient httpClient = new HttpClient( sslContextFactory );

		httpClient.setAddressResolutionTimeout( getAddressResolutionTimeout() );
		httpClient.setBindAddress( getBindAddress() );
		final ByteBufferPool byteBufferPool = getByteBufferPool();
		if( byteBufferPool != null )
			httpClient.setByteBufferPool( byteBufferPool );
		httpClient.setConnectTimeout( getConnectTimeout() );
		final CookieStore cookieStore = getCookieStore();
		if( cookieStore != null )
			httpClient.setCookieStore( cookieStore );
		httpClient.setExecutor( getExecutor() );
		httpClient.setFollowRedirects( isFollowRedirects() );
		httpClient.setIdleTimeout( getIdleTimeout() );
		httpClient.setMaxConnectionsPerDestination( getMaxConnectionsPerDestination() );
		httpClient.setMaxRedirects( getMaxRedirects() );
		httpClient.setMaxRequestsQueuedPerDestination( getMaxRequestsQueuedPerDestination() );
		httpClient.setRemoveIdleDestinations( getRemoveIdleDestinations() );
		httpClient.setRequestBufferSize( getRequestBufferSize() );
		httpClient.setResponseBufferSize( getResponseBufferSize() );
		httpClient.setScheduler( getScheduler() );
		httpClient.setStopTimeout( getStopTimeout() );
		httpClient.setStrictEventOrdering( isStrictEventOrdering() );
		httpClient.setTCPNoDelay( isTcpNoDelay() );
		final String userAgentField = getUserAgentField();
		if( userAgentField != null )
			httpClient.setUserAgentField( new HttpField( HttpHeader.USER_AGENT, userAgentField ) );

		return httpClient;
	}

	/**
	 * The wrapped Jetty HTTP client.
	 */
	private volatile HttpClient httpClient;
}
