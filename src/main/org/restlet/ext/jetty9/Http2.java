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

import org.restlet.data.Protocol;

/**
 * HTTP/2 utilities.
 * 
 * @author Tal Liron
 */
public class Http2
{
	/** HTTPS protocol version 2 (Secure). */
	public static final Protocol HTTPS_PROTOCOL = new Protocol( "https", "HTTPS", "HTTP", "HyperText Transport Protocol (Secure)", 443, true, "2" );

	/** HTTP protocol version 2 (cleartext). */
	public static final Protocol HTTP_PROTOCOL = new Protocol( "http", "HTTP", "HyperText Transport Protocol", 80, "2" );

	/**
	 * These TLS 1.2 cipher suites are blacklisted by the HTTP/2 spec. Clients
	 * should reject servers that use any of these suites.
	 * 
	 * @see <a href="https://http2.github.io/http2-spec/#BadCipherSuites">HTTP/2
	 *      TLS 1.2 Cipher Suite Black List</a>
	 */
	public static final String[] TLS_BAD_CIPHER_SUITES;

	static
	{
		final ArrayList<String> ciphers = new ArrayList<String>();
		final String name = Http2.class.getPackage().getName().replaceAll( "\\.", "/" ) + "/internal/http2_tls_bad_cipher_suites.txt";
		final BufferedReader reader = new BufferedReader( new InputStreamReader( Http2.class.getClassLoader().getResourceAsStream( name ) ) );
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
		TLS_BAD_CIPHER_SUITES = ciphers.toArray( new String[ciphers.size()] );
	}
}
