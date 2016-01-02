Jetty 9 connector for Restlet 2.2+
==================================

This Restlet extension allows you to use [Jetty](http://www.eclipse.org/jetty/) 9.3 as both a
server and client connector, for both HTTP and HTTPS. HTTP/2 is supported for the server connector.

Note that Jetty 9.3 requires a JVM of at least version 8.

See also [this issue](https://github.com/restlet/restlet-framework-java/issues/1108) and
the [Restlet forum](http://restlet.tigris.org/ds/viewMessage.do?dsForumId=4447&dsMessageId=3067974).


HTTP/2 Server
-------------

1) Dependencies: Jetty [HTTP/2 server](http://mvnrepository.com/artifact/org.eclipse.jetty.http2/http2-server)
and [HPACK](http://mvnrepository.com/artifact/org.eclipse.jetty.http2/http2-hpack), as well as an
[ALPN boot solution](http://mvnrepository.com/artifact/org.mortbay.jetty.alpn/alpn-boot).

2) The ALPN boot jar needs to be added to the [JVM's boot classpath](https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html).
E.g.:

    java -Xbootclasspath/p:<path_to_alpn_boot_jar> ...

3) You need to configure your [Server](http://restlet.com/technical-resources/restlet-framework/javadocs/2.3/jse/api/index.html?org/restlet/Server.html)
for HTTP/2. As of Restlet 2.3, the [Protocol class](http://restlet.com/technical-resources/restlet-framework/javadocs/2.3/jse/api/index.html?org/restlet/data/Protocol.html)
does not have HTTP version 2 constants, but they are available in this driver in the [Http2 utility class]
(http://threecrickets.com/api/java/restlet-jetty9/index.html?org/restlet/ext/jetty9/Http2.html). Here's some pseudo-code to create a server
that supports both HTTP/2 and legacy HTTP/1.1:

    import org.restlet.Server;
    import org.restlet.ext.jetty9.Http2;
    Server server = new Server(Http2.HTTPS_PROTOCOL, 443)
    server.getProtocols().add(Protocol.HTTPS)
    component.getServers().add(server)


HTTP/2 Cleartext Server
-----------------------

Supported for servers only via the [Http2.HTTP_PROTOCOL constant](http://threecrickets.com/api/java/restlet-jetty9/index.html?org/restlet/ext/jetty9/Http2.html).

Note that as Jetty 9.3, it does not support HTTP/2 cleartext for *clients*. (Generally speaking, it's unclear at this point if
the industry will embrace this variant, and it's difficult to find tools to test it.)


Get It
------

You can download the latest binary and API documentation jars from
[here](http://repository.threecrickets.com/maven/org/restlet/jse/org.restlet.ext.jetty9/).

You can browse the latest API documentation [here](http://threecrickets.com/api/java/restlet-jetty9/).

To install via Maven:

	<repository>
		<id>three-crickets</id>  
		<name>Three Crickets Repository</name>  
		<url>http://repository.threecrickets.com/maven/</url>  
	</repository>
	
	<dependency>
		<groupId>org.restlet.jse</groupId>
		<artifactId>org.restlet.ext.jetty9</artifactId>
		<version>[2.3-dev1,2.4)</version>
	</dependency>

Hosted by [Three Crickets](http://threecrickets.com/repository/).


Build It
--------

The repository includes an Eclipse project. Build using Maven.

Legal
-----

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

The code is copyright Three Crickets LLC and Restlet S.A.S.

"Restlet" is a registered trademark of [Restlet S.A.S.](http://restlet.com/legal/).
