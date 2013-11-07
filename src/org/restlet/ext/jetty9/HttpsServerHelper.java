/**
 * Copyright 2005-2013 Restlet S.A.S. The contents of this file are subject to
 * the terms of one of the following open source licenses: Apache 2.0 or LGPL
 * 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the "Licenses"). You can select the
 * license that you prefer but you may not use this file except in compliance
 * with one of these Licenses. You can obtain a copy of the Apache 2.0 license
 * at http://www.opensource.org/licenses/apache-2.0 You can obtain a copy of the
 * LGPL 3.0 license at http://www.opensource.org/licenses/lgpl-3.0 You can
 * obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1 You can obtain a copy of the CDDL
 * 1.0 license at http://www.opensource.org/licenses/cddl1 You can obtain a copy
 * of the EPL 1.0 license at http://www.opensource.org/licenses/eclipse-1.0 See
 * the Licenses for the specific language governing permissions and limitations
 * under the Licenses. Alternatively, you can obtain a royalty free commercial
 * license with less limitations, transferable or non-transferable, directly at
 * http://www.restlet.com/products/restlet-framework Restlet is a registered
 * trademark of Restlet S.A.S.
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
