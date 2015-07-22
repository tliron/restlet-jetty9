Jetty 9 connector for Restlet 2.2 and 2.3
=========================================

This Restlet extension allows you to use [Jetty](http://www.eclipse.org/jetty/) 9.3 as both a
server and client connector, for both HTTP and HTTPS. HTTP/2 is supported for the server connector.

As discussed in the [Restlet forum](http://restlet.tigris.org/ds/viewMessage.do?dsForumId=4447&dsMessageId=3067974).

The repository includes an Eclipse project. Build using Maven.

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

Legal
-----

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

The code is copyright Three Crickets LLC and Restlet S.A.S.

"Restlet" is a registered trademark of [Restlet S.A.S.](http://restlet.org/download/legal).
