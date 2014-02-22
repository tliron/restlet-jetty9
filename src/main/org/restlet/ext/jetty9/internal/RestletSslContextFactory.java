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
import java.net.InetAddress;
import java.util.logging.Level;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.restlet.Context;

/**
 * Jetty SSL context factory based on a Restlet SSL context one.
 * 
 * @author Jerome Louvel
 * @author Tal Liron
 */
public class RestletSslContextFactory extends SslContextFactory
{
	/**
	 * Constructor.
	 * 
	 * @param restletSslContextFactory
	 *        The Restlet SSL context factory to leverage.
	 * @throws Exception
	 */
	public RestletSslContextFactory( org.restlet.engine.ssl.SslContextFactory restletSslContextFactory ) throws Exception
	{
		setSslContext( restletSslContextFactory.createSslContext() );
	}

	@Override
	public void checkKeyStore()
	{
		try
		{
			if( getSslContext() == null )
				super.checkKeyStore();
		}
		catch( IllegalStateException e )
		{
			Context.getCurrentLogger().log( Level.FINE, "Unable to check Jetty SSL keystore", e );
		}
	}

	@Override
	public SSLEngine newSSLEngine()
	{
		return getSslContext().createSSLEngine();
	}

	@Override
	public SSLEngine newSSLEngine( String host, int port )
	{
		return getSslContext().createSSLEngine( host, port );
	}

	@Override
	public SSLServerSocket newSslServerSocket( String host, int port, int backlog ) throws IOException
	{
		final SSLServerSocketFactory factory = getSslContext().getServerSocketFactory();
		return (SSLServerSocket) ( ( host == null ) ? factory.createServerSocket( port, backlog ) : factory.createServerSocket( port, backlog, InetAddress.getByName( host ) ) );
	}

	@Override
	public SSLSocket newSslSocket() throws IOException
	{
		return (SSLSocket) getSslContext().getSocketFactory().createSocket();
	}
}
