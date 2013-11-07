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

import org.eclipse.jetty.server.ServerConnector;
import org.restlet.Server;
import org.restlet.data.Protocol;

/**
 * Jetty HTTP server connector. Here is the list of additional parameters that
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
 * <td>1</td>
 * <td>The type of Jetty connector to use.<br>
 * 1 : Selecting NIO connector (Jetty's SelectChannelConnector class).<br>
 * 2 : Blocking NIO connector (Jetty's BlockingChannelConnector class).<br>
 * 3 : Blocking BIO connector (Jetty's SocketConnector class).</td>
 * </tr>
 * </table>
 * 
 * @see <a href="http://jetty.mortbay.org/jetty6/">Jetty home page</a>
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class HttpServerHelper extends JettyServerHelper
{
	/**
	 * Constructor.
	 * 
	 * @param server
	 *        The server to help.
	 */
	public HttpServerHelper( Server server )
	{
		super( server );
		getProtocols().add( Protocol.HTTP );
	}

	/**
	 * Creates a new internal Jetty connector.
	 * 
	 * @return A new internal Jetty connector.
	 */
	@Override
	protected ServerConnector createConnector( org.eclipse.jetty.server.Server server )
	{
		// Create and configure the Jetty HTTP connector
		return new ServerConnector( server, getAcceptorThreads(), 0 );
	}
}
