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

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.restlet.Server;
import org.restlet.data.Protocol;

/**
 * Jetty 9 HTTP server connector.
 * 
 * @see <a href="http://www.eclipse.org/jetty/">Jetty home page</a>
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
	 * Creates a new Jetty connection factory.
	 * 
	 * @param configuration
	 *        The HTTP configuration.
	 * @return A new Jetty connection factory.
	 */
	protected ConnectionFactory createConnectionFactory( HttpConfiguration configuration )
	{
		// Create and configure the Jetty HTTP connector
		return new HttpConnectionFactory( configuration );
	}
}
