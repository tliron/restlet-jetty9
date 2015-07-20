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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;

import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.engine.ssl.DefaultSslContextFactory;
import org.restlet.engine.ssl.SslUtils;

/**
 * Jetty 9 HTTPS server connector. Here is the list of additional parameters
 * that are supported. They should be set in the Server's context before it is
 * started:
 * <table summary="parameters">
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
 * <td>Lets you specify a {@link org.restlet.engine.ssl.SslContextFactory}
 * qualified class name as a parameter, or an instance as an attribute for a
 * more complete and flexible SSL context setting</td>
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
	 * These TLS 1.2 cipher suites are blacklisted by the HTTP/2 spec. Clients
	 * should reject servers that use any of these suites.
	 * 
	 * @see <a href="https://http2.github.io/http2-spec/#BadCipherSuites">HTTP/2
	 *      TLS 1.2 Cipher Suite Black List</a>
	 */
	public static final String[] HTTP2_TLS_BAD_CIPHER_SUITES;

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
	 * Creates new internal Jetty connection factories.
	 * 
	 * @param configuration
	 *        The HTTP configuration.
	 * @return New internal Jetty connection factories.
	 */
	protected ConnectionFactory[] createConnectionFactories( HttpConfiguration configuration )
	{
		ConnectionFactory[] connectionFactories = super.createConnectionFactories( configuration );

		try
		{
			final org.restlet.engine.ssl.SslContextFactory sslContextFactory = SslUtils.getSslContextFactory( this );
			final SslContextFactory jettySslContextFactory = new SslContextFactory();
			jettySslContextFactory.setSslContext( sslContextFactory.createSslContext() );

			if( this.getHttp2() || this.getHttp2c() )
				// Make sure not to use blacklisted cipher suites
				jettySslContextFactory.setExcludeCipherSuites( HTTP2_TLS_BAD_CIPHER_SUITES );

			return AbstractConnectionFactory.getFactories( jettySslContextFactory, connectionFactories );
		}
		catch( Exception e )
		{
			getLogger().log( Level.WARNING, "Unable to create the Jetty SSL context factory", e );
			return null;
		}
	}

	static
	{
		final ArrayList<String> ciphers = new ArrayList<String>();
		final String name = HttpsServerHelper.class.getPackage().getName().replaceAll( "\\.", "/" ) + "/http2_tls_bad_cipher_suites.txt";
		final BufferedReader reader = new BufferedReader( new InputStreamReader( HttpsServerHelper.class.getClassLoader().getResourceAsStream( name ) ) );
		try
		{
			try
			{
				String line = reader.readLine();
				while( line != null )
				{
					ciphers.add( line.trim() );
					line = reader.readLine();
				}
			}
			finally
			{
				reader.close();
			}
		}
		catch( IOException e )
		{
			throw new RuntimeException( e );
		}
		HTTP2_TLS_BAD_CIPHER_SUITES = ciphers.toArray( new String[ciphers.size()] );
	}
}
