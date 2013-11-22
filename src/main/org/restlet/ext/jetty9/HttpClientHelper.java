/**
 * Copyright 2013 Three Crickets LLC and Restlet S.A.S.
 * <p>
 * The contents of this file are subject to the terms of the Jetty 2.0 license:
 * http://www.opensource.org/licenses/apache-2.0
 * <p>
 * This code is a derivative of code that is copyright 2005-2013 Restlet S.A.S.,
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
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Scheduler;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.data.Protocol;
import org.restlet.engine.adapter.ClientCall;
import org.restlet.engine.util.ReferenceUtils;
import org.restlet.ext.jetty9.internal.JettyClientCall;
import org.restlet.ext.jetty9.internal.RestletSslContextFactory;
import org.restlet.ext.ssl.DefaultSslContextFactory;

/**
 * HTTP client connector using the Jetty project.<br>
 * <br>
 * Here is the list of parameters that are supported. They should be set in the
 * Client's context before it is started:
 * <table>
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
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>bindAddress</td>
 * <td>String</td>
 * <td>null</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>bindPort</td>
 * <td>int</td>
 * <td>null</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>connectTimeout</td>
 * <td>long</td>
 * <td>15000</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>dispatchIo</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>followRedirects</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>idleTimeout</td>
 * <td>long</td>
 * <td>0</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>maxConnectionsPerDestination</td>
 * <td>int</td>
 * <td>64</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>maxRedirects</td>
 * <td>int</td>
 * <td>8</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>maxRequestsQueuedPerDestination</td>
 * <td>int</td>
 * <td>1024</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>requestBufferSize</td>
 * <td>int</td>
 * <td>4096</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>responseBufferSize</td>
 * <td>int</td>
 * <td>16384</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>stopTimeout</td>
 * <td>long</td>
 * <td>30000</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>strictEventOrdering</td>
 * <td>boolean</td>
 * <td>false</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>tcpNoDelay</td>
 * <td>boolean</td>
 * <td>true</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>userAgentField</td>
 * <td>String</td>
 * <td>null</td>
 * <td>DESC.</td>
 * </tr>
 * <tr>
 * <td>sslContextFactory</td>
 * <td>String</td>
 * <td>org.restlet.ext.ssl.DefaultSslContextFactory</td>
 * <td>Let you specify a {@link SslContextFactory} qualified class name as a
 * parameter, or an instance as an attribute for a more complete and flexible
 * SSL context setting.</td>
 * </tr>
 * </table>
 * For the default SSL parameters see the Javadocs of the
 * {@link DefaultSslContextFactory} class.
 * 
 * @see <a href="http://www.eclipse.org/jetty/">Jetty home page</a>
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class HttpClientHelper extends org.restlet.engine.adapter.HttpClientHelper
{
	/**
	 * Constructor.
	 * 
	 * @param client
	 *        The client to help.
	 */
	public HttpClientHelper( Client client )
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
			result = new JettyClientCall( this, request.getMethod().toString(), ReferenceUtils.update( request.getResourceRef(), request ).toString(), request.isEntityAvailable() );
		}
		catch( IOException e )
		{
			getLogger().log( Level.WARNING, "Unable to create the HTTP client call", e );
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
		return this.httpClient;
	}

	@Override
	public void start() throws Exception
	{
		super.start();

		if( this.httpClient == null )
			this.httpClient = createHttpClient();

		final HttpClient httpClient = getHttpClient();
		if( httpClient != null )
		{
			getLogger().info( "Starting the Jetty HTTP client" );
			httpClient.start();
		}
	}

	@Override
	public void stop() throws Exception
	{
		final HttpClient httpClient = getHttpClient();
		if( httpClient != null )
		{
			getLogger().info( "Stopping the Jetty HTTP client" );
			httpClient.stop();
		}

		super.stop();
	}

	/**
	 * Defaults to 15000.
	 * 
	 * @return The address resolution timeout.
	 */
	public long getAddressResolutionTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "addressResolutionTimeout", "15000" ) );
	}

	/**
	 * Both default to null.
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
	 * Defaults to 15000.
	 * 
	 * @return The connect timeout.
	 */
	public long getConnectTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "connectTimeout", "15000" ) );
	}

	/**
	 * Defaults to true.
	 * 
	 * @return Whether to dispatch I/O.
	 */
	public boolean isDispatchIO()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "dispatchIo", "true" ) );
	}

	/**
	 * Defaults to true.
	 * 
	 * @return Whether to follow redirects.
	 */
	public boolean isFollowRedirects()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "followRedirects", "true" ) );
	}

	/**
	 * Defaults to 0.
	 * 
	 * @return The idle timeout.
	 */
	public long getIdleTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "idleTimeout", "0" ) );
	}

	/**
	 * Defaults to 64.
	 * 
	 * @return The maximum connections per destination.
	 */
	public int getMaxConnectionsPerDestination()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxConnectionsPerDestination", "64" ) );
	}

	/**
	 * Defaults to 8.
	 * 
	 * @return The maximum redirects.
	 */
	public int getMaxRedirects()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxRedirects", "8" ) );
	}

	/**
	 * Defaults to 1024.
	 * 
	 * @return The maximum requests queues per destination.
	 */
	public int getMaxRequestsQueuedPerDestination()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "maxRequestsQueuedPerDestination", "1024" ) );
	}

	/**
	 * Defaults to 4096.
	 * 
	 * @return The request buffer size.
	 */
	public int getRequestBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "requestBufferSize", "4096" ) );
	}

	/**
	 * Defaults to 16384.
	 * 
	 * @return The response buffer size.
	 */
	public int getResponseBufferSize()
	{
		return Integer.parseInt( getHelpedParameters().getFirstValue( "responseBufferSize", "16384" ) );
	}

	/**
	 * Defaults to 30000.
	 * 
	 * @return The stop timeout.
	 */
	public long getStopTimeout()
	{
		return Long.parseLong( getHelpedParameters().getFirstValue( "stopTimeout", "30000" ) );
	}

	/**
	 * Defaults to false.
	 * 
	 * @return Whether event ordering is string.
	 */
	public boolean isStrictEventOrdering()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "strictEventOrdering", "false" ) );
	}

	/**
	 * Defaults to true.
	 * 
	 * @return Whether to enable TCP no-delay.
	 */
	public boolean isTcpNoDelay()
	{
		return Boolean.parseBoolean( getHelpedParameters().getFirstValue( "tcpNoDelay", "true" ) );
	}

	/**
	 * Defaults to null.
	 * 
	 * @return The user agent field or null.
	 */
	public String getUserAgentField()
	{
		return getHelpedParameters().getFirstValue( "userAgentField", null );
	}

	/**
	 * Defaults to null.
	 * 
	 * @return The cookie store.
	 */
	public CookieStore getCookieStore()
	{
		return null;
	}

	/**
	 * Defaults to null.
	 * 
	 * @return The executor.
	 */
	public Executor getExecutor()
	{
		return null;
	}

	/**
	 * Defaults to null.
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
			sslContextFactory = new RestletSslContextFactory( org.restlet.ext.ssl.internal.SslUtils.getSslContextFactory( this ) );
		}
		catch( Exception e )
		{
		}

		final HttpClient httpClient = new HttpClient( sslContextFactory );

		httpClient.setAddressResolutionTimeout( getAddressResolutionTimeout() );
		httpClient.setBindAddress( getBindAddress() );
		httpClient.setConnectTimeout( getConnectTimeout() );
		final CookieStore cookieStore = getCookieStore();
		if( cookieStore != null )
			httpClient.setCookieStore( cookieStore );
		httpClient.setDispatchIO( isDispatchIO() );
		httpClient.setExecutor( getExecutor() );
		httpClient.setFollowRedirects( isFollowRedirects() );
		httpClient.setIdleTimeout( getIdleTimeout() );
		httpClient.setMaxConnectionsPerDestination( getMaxConnectionsPerDestination() );
		httpClient.setMaxRedirects( getMaxRedirects() );
		httpClient.setMaxRequestsQueuedPerDestination( getMaxRequestsQueuedPerDestination() );
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
