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
