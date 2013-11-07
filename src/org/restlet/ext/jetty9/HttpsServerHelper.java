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

import java.util.logging.Level;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.ext.jetty9.internal.RestletSslContextFactory;
import org.restlet.ext.ssl.DefaultSslContextFactory;

/**
 * Jetty HTTPS server connector. Here is the list of additional parameters that
 * are supported. They should be set in the Server's context before it is
 * started:
 * <table>
 * <tr>
 * <th>Parameter name</th>
 * <th>Value type</th>
 * <th>Default value</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>type</td>
 * <td>int</td>
 * <td>2</td>
 * <td>The type of Jetty connector to use.<br>
 * 1 : Selecting NIO connector (Jetty's SslSelectChannelConnector class).<br>
 * 2 : Blocking BIO connector (Jetty's SslSocketConnector class)</td>
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
 * @see <a
 *      href="http://docs.codehaus.org/display/JETTY/How+to+configure+SSL">How
 *      to configure SSL for Jetty</a>
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class HttpsServerHelper extends JettyServerHelper
{
	/**
	 * Constructor.
	 * 
	 * @param server
	 *        The server to help.
	 */
	public HttpsServerHelper( Server server )
	{
		super( server );
		getProtocols().add( Protocol.HTTPS );
	}

	/**
	 * Creates a new internal Jetty connector.
	 * 
	 * @return A new internal Jetty connector.
	 */
	@Override
	protected ServerConnector createConnector( org.eclipse.jetty.server.Server server )
	{
		SslContextFactory sslContextFactory = null;

		try
		{
			sslContextFactory = new RestletSslContextFactory( org.restlet.ext.ssl.internal.SslUtils.getSslContextFactory( this ) );
		}
		catch( Exception e )
		{
			getLogger().log( Level.WARNING, "Unable to create the Jetty SSL context factory", e );
		}

		if( sslContextFactory != null )
		{
			// Create and configure the Jetty HTTP connector
			return new ServerConnector( server, getAcceptorThreads(), 0, sslContextFactory );
		}

		return null;
	}
}
