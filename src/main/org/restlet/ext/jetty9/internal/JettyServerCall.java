/**
 * Copyright 2014-2016 Three Crickets LLC and Restlet S.A.S.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.restlet.Response;
import org.restlet.Server;
import org.restlet.data.Header;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.adapter.ServerCall;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.util.Series;

/**
 * Call that is used by the Jetty HTTP server connectors.
 * 
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class JettyServerCall extends ServerCall
{
	/**
	 * Constructor.
	 * 
	 * @param server
	 *        The parent server.
	 * @param channel
	 *        The wrapped Jetty HTTP channel.
	 * @param ensureHostHeader
	 *        Whether to generate a Host header if not provided by the request
	 */
	public JettyServerCall( Server server, HttpChannel channel, boolean ensureHostHeader )
	{
		super( server );
		this.channel = channel;
		this.ensureHostHeader = ensureHostHeader;
	}

	/**
	 * Closes the end point.
	 */
	public boolean abort()
	{
		getChannel().getEndPoint().close();
		return true;
	}

	@Override
	public void complete()
	{
		final org.eclipse.jetty.server.Response response = getChannel().getResponse();

		// Flush the response
		try
		{
			response.flushBuffer();
		}
		catch( IOException e )
		{
			getLogger().log( Level.FINE, "Unable to flush the response", e );
		}

		// Fully complete the response
		try
		{
			response.closeOutput();
		}
		catch( IOException e )
		{
			getLogger().log( Level.FINE, "Unable to complete the response", e );
		}
	}

	@Override
	public void flushBuffers() throws IOException
	{
		getChannel().getResponse().flushBuffer();
	}

	@Override
	public List<Certificate> getCertificates()
	{
		final Object certificateArray = getChannel().getRequest().getAttribute( "javax.servlet.request.X509Certificate" );
		if( certificateArray instanceof Certificate[] )
			return Arrays.asList( (Certificate[]) certificateArray );
		return null;
	}

	@Override
	public String getCipherSuite()
	{
		final Object cipherSuite = getChannel().getRequest().getAttribute( "javax.servlet.request.cipher_suite" );
		if( cipherSuite instanceof String )
			return (String) cipherSuite;
		return null;
	}

	@Override
	public String getClientAddress()
	{
		return getChannel().getRequest().getRemoteAddr();
	}

	@Override
	public int getClientPort()
	{
		return getChannel().getRequest().getRemotePort();
	}

	/**
	 * Returns the wrapped Jetty HTTP channel.
	 * 
	 * @return The wrapped Jetty HTTP channel.
	 */
	public HttpChannel getChannel()
	{
		return channel;
	}

	/**
	 * Returns the request method.
	 * 
	 * @return The request method.
	 */
	@Override
	public String getMethod()
	{
		return getChannel().getRequest().getMethod();
	}

	public InputStream getRequestEntityStream( long size )
	{
		try
		{
			return getChannel().getRequest().getInputStream();
		}
		catch( IOException e )
		{
			getLogger().log( Level.WARNING, "Unable to get request entity stream", e );
			return null;
		}
	}

	/**
	 * Returns the list of request headers.
	 * 
	 * @return The list of request headers.
	 */
	@Override
	public Series<Header> getRequestHeaders()
	{
		final Series<Header> result = super.getRequestHeaders();

		if( !requestHeadersAdded )
		{
			// Copy the headers from the request object
			final Request request = getChannel().getRequest();
			for( Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); )
			{
				final String headerName = names.nextElement();
				for( Enumeration<String> values = request.getHeaders( headerName ); values.hasMoreElements(); )
				{
					final String headerValue = values.nextElement();
					result.add( headerName, headerValue );
				}
			}

			// HTTP/2 does not have a Host header
			// See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=473118
			if( ensureHostHeader && ( result.getFirstValue( HeaderConstants.HEADER_HOST ) == null ) )
			{
				final String scheme = request.getScheme();
				final String server = request.getServerName();
				final int port = request.getServerPort();

				if( scheme.equalsIgnoreCase( Protocol.HTTP.getSchemeName() ) && ( port == Protocol.HTTP.getDefaultPort() )
					|| scheme.equalsIgnoreCase( Protocol.HTTPS.getSchemeName() ) && ( port == Protocol.HTTPS.getDefaultPort() ) )
					result.set( HeaderConstants.HEADER_HOST, server );
				else
					result.set( HeaderConstants.HEADER_HOST, server + ":" + port );
			}

			requestHeadersAdded = true;
		}

		return result;
	}

	public InputStream getRequestHeadStream()
	{
		// Not available
		return null;
	}

	/**
	 * Returns the URI on the request line (most like a relative reference, but
	 * not necessarily).
	 * 
	 * @return The URI on the request line.
	 */
	@Override
	public String getRequestUri()
	{
		return getChannel().getRequest().getHttpURI().getPathQuery();
	}

	/**
	 * Returns the response stream if it exists.
	 * 
	 * @return The response stream if it exists.
	 */
	public OutputStream getResponseEntityStream()
	{
		try
		{
			return getChannel().getResponse().getOutputStream();
		}
		catch( IOException e )
		{
			getLogger().log( Level.WARNING, "Unable to get response entity stream", e );
			return null;
		}
	}

	/**
	 * Returns the response address.<br>
	 * Corresponds to the IP address of the responding server.
	 * 
	 * @return The response address.
	 */
	@Override
	public String getServerAddress()
	{
		return getChannel().getRequest().getLocalAddr();
	}

	@Override
	public Integer getSslKeySize()
	{
		final Object keySize = getChannel().getRequest().getAttribute( "javax.servlet.request.key_size" );
		if( keySize instanceof Number )
			return ( (Number) keySize ).intValue();
		return super.getSslKeySize();
	}

	@Override
	public String getSslSessionId()
	{
		final Object sessionId = getChannel().getRequest().getAttribute( "javax.servlet.request.ssl_session_id" );
		if( sessionId instanceof String )
			return (String) sessionId;
		return null;
	}

	/**
	 * Indicates if the request was made using a confidential mean.<br>
	 * 
	 * @return True if the request was made using a confidential mean.<br>
	 */
	@Override
	public boolean isConfidential()
	{
		return getChannel().getRequest().isSecure();
	}

	@Override
	public boolean isConnectionBroken( Throwable exception )
	{
		return ( exception instanceof EofException ) || super.isConnectionBroken( exception );
	}

	@Override
	public void sendResponse( Response response ) throws IOException
	{
		final org.eclipse.jetty.server.Response jettyResponse = getChannel().getResponse();

		// Add call headers
		for( Header header : getResponseHeaders() )
			jettyResponse.addHeader( header.getName(), header.getValue() );

		// Set the status code in the response. We do this after adding the
		// headers because when we have to rely on the 'sendError' method,
		// the Servlet containers are expected to commit their response.
		if( Status.isError( getStatusCode() ) && ( response.getEntity() == null ) )
		{
			try
			{
				jettyResponse.sendError( getStatusCode(), getReasonPhrase() );
			}
			catch( IOException e )
			{
				getLogger().log( Level.WARNING, "Unable to set the response error status", e );
			}
		}
		else
		{
			// Send the response entity
			jettyResponse.setStatus( getStatusCode() );
			super.sendResponse( response );
		}
	}

	/** The wrapped Jetty HTTP channel. */
	private final HttpChannel channel;

	/** Whether to generate a Host header if not provided by the request. */
	private final boolean ensureHostHeader;

	/** Indicates if the request headers were parsed and added. */
	private volatile boolean requestHeadersAdded;
}
