/**
 * Copyright 2014 Three Crickets LLC and Restlet S.A.S.
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

import java.util.logging.Level;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.ext.jetty9.internal.RestletSslContextFactory;
import org.restlet.ext.ssl.DefaultSslContextFactory;
import org.restlet.ext.ssl.SslContextFactory;

/**
 * Jetty 9 HTTPS server connector. Here is the list of additional parameters
 * that are supported. They should be set in the Server's context before it is
 * started:
 * <table>
 * <tr>
 * <th>Parameter name</th>
 * <th>Value type</th>
 * <th>Default value</th>
 * <th>Description</th>
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
 * @see <a href="http://wiki.eclipse.org/Jetty/Howto/Configure_SSL">How to
 *      configure SSL for Jetty</a>
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
	 * Creates a new Jetty connection factory.
	 * 
	 * @param configuration
	 *        The HTTP configuration.
	 * @return A new Jetty connection factory.
	 */
	protected ConnectionFactory createConnectionFactory( HttpConfiguration configuration )
	{
		try
		{
			final org.eclipse.jetty.util.ssl.SslContextFactory sslContextFactory = new RestletSslContextFactory( org.restlet.ext.ssl.internal.SslUtils.getSslContextFactory( this ) );
			return new SslConnectionFactory( sslContextFactory, "http" );
		}
		catch( Exception e )
		{
			getLogger().log( Level.WARNING, "Unable to create the Jetty SSL context factory", e );
			return null;
		}
	}
}
