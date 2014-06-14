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

package org.restlet.ext.jetty9.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.restlet.Server;
import org.restlet.ext.jetty9.HttpServerHelper;
import org.restlet.ext.jetty9.HttpsServerHelper;
import org.restlet.ext.jetty9.JettyServerHelper;

/**
 * Jetty handler that knows how to convert Jetty calls into Restlet calls. This
 * handler isn't a full server, if you use it you need to manually setup the
 * Jetty server connector and add this handler to a Jetty server.
 * 
 * @author Valdis Rigdon
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class JettyHandler extends AbstractHandler
{
	/**
	 * Constructor for HTTP server connectors.
	 * 
	 * @param server
	 *        Restlet HTTP server connector.
	 */
	public JettyHandler( Server server )
	{
		this( server, false );
	}

	/**
	 * Constructor for HTTP server connectors.
	 * 
	 * @param server
	 *        Restlet server connector.
	 * @param secure
	 *        Indicates if the server supports HTTP or HTTPS.
	 */
	public JettyHandler( Server server, boolean secure )
	{
		if( secure )
			this.helper = new HttpsServerHelper( server );
		else
			this.helper = new HttpServerHelper( server );
	}

	/**
	 * Handles a Jetty call by converting it to a Restlet call and giving it for
	 * processing to the Restlet server.
	 * 
	 * @param target
	 *        The target of the request, either a URI or a name.
	 * @param request
	 *        The Jetty request.
	 * @param servletRequest
	 *        The Servlet request.
	 * @param servletResponse
	 *        The Servlet response.
	 */
	public void handle( String target, Request request, HttpServletRequest servletRequest, HttpServletResponse servletResponse ) throws IOException, ServletException
	{
		final HttpChannel<?> channel = HttpChannel.getCurrentHttpChannel();
		final Request baseRequest = ( servletRequest instanceof Request ) ? (Request) servletRequest : channel.getRequest();
		this.helper.handle( new JettyServerCall( this.helper.getHelped(), channel ) );
		baseRequest.setHandled( true );
	}

	@Override
	protected void doStart() throws Exception
	{
		super.doStart();
		this.helper.start();
	}

	@Override
	protected void doStop() throws Exception
	{
		super.doStop();
		this.helper.stop();
	}

	/** The Restlet server helper. */
	private final JettyServerHelper helper;
}
