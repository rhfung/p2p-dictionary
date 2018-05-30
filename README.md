P2P Dictionary
==============

P2P Dictionary is a distributed key-value store for multiple nodes
on a local area network. Each node will subscribe to 
a subset of key-value pairs. Key-value pairs are replicated as necessary
between nodes to reach to another node. Similar to most 
NoSQL implementations, it does not provide an SQL interface or
guarantee ACID (atomicity, consistency, isolation, durability).

P2P dictionary will run on a local area network discovered using 
LAN discovery technologies (e.g., Apple Bonjour, Zeroconf, UDP broadcast)
or any reachable IP address in a public network. Peer links can be discovered
or connected by the client.

P2P dictionary provides a server written in 
[.NET Framework](https://github.com/rhfung/p2p-dictionary-csharp), 
[.NET Core](https://github.com/rhfung/p2p-dictionary-csharp),
and [Java JVM](https://github.com/rhfung/p2p-dictionary). 
A REST interface is provided by the P2P server for read-only access
to key-value pairs. A web interface is provided for web browser access 
to key-value pairs stored on each node. A redistributable package is provided
using Docker containers with both .NET and Java implementations.

Copyright (C) 2011-2018, Richard H Fung

License
-------

You agree to the LICENSE before using this Software.

Basic requirements
------------------

This project can be built using Docker or local machine.

## Docker
* Docker 17.05+

## Local Machine
* Java SE 8 or higher

* Gradle 3.4

* For Bonjour discovery on Windows:
  * Apple Bonjour Print Services for Windows: http://support.apple.com/kb/DL999
  * DNS_SD.jar from Apple Bonjour (Windows)
  * Windows computer

Example Usage
-------------

Taken from examples/first-example/TestRun.java:

    import com.rhfung.P2PDictionary.*;

    // ...

    P2PDictionary node = P2PDictionary.builder()
            .setDescription("test")
            .setNamespace("test")
            .setPort(3333)
            .setServerMode(P2PDictionaryServerMode.AutoRegister)
            .setClientMode(P2PDictionaryClientMode.AutoConnect)
            .setClientSearchTimespan(1500)
            .setLogLevel(System.out, LogInstructions.DEBUG)
            .build();

    // ...

    node.put("message1", "hello world");

    // ...

    Object message1 = node.get("message1");

    // ...

    node.close();

See other sample projects in `examples`

Running in Docker
-----------------

To run/build this project in a container, run the following commands from your Docker terminal:

    ./start_docker [parameters]

And then visit the URL ```http://localhost:8765```.

The Docker-related configuration files are:

* start_docker
* .dockerignore
* Dockerfile

The Docker image is built with Gradle to compile the Java source.

Building on Local Machine
------------------------

Gradle is a modern way to build Java source code.
1. Ensure `gradle` is installed
2. Run `gradle build`

This project can also be built using IntelliJ IDEA. Support for IntelliJ will be removed in the future.

## Running JAR

If you have Java 7+ already installed in your system path, you can run the compiled JAR using:

    ./start [parameters]

By default it runs on ```http://localhost:8765```.

### Building Examples

1. Use `gradle build` to build dependencies.
2. Use the `start` script in each example to compile and run the project.

P2PD CLI Parameters
------------------

CLI Parameters allowed for the Docker/JAR command line interface:

     -d,--discovery <arg>     Backend discovery mechanism: none, bonjour,
                              win-bonjour, hello. Default: hello
        --debug               Enable debugging mode
        --fulldebug           Enable debugging mode
     -h,--help <arg>          Show this help
     -m,--description <arg>   Description for the server
     -n,--node <host:port>    Provide clients in the form
                              hostname:port,hostname:port,... (separated by
                              commas)
        --nopattern           Monitors no patterns
     -s,--namespace <arg>     Namespace for the server
     -p, --port <arg>         Bind to port default:8765
        --pattern <arg>       Monitors a specific pattern using wildcard (*),
                              single character (?), and number (#)
                              placeholders; default to *
     -t,--timespan <arg>      Search interval for clients in milliseconds


Related Projects
----------------

I forked this project from a graduate-level course on peer to peer networking.
I moved the original implementation, written in C# for the .NET platform,
into [another repository](https://github.com/rhfung/p2p-dictionary-csharp).

Known Issues
------------

Issues in 3.1+:

* Duplicate connections aren't closed automatically
* win-bonjour discovery module doesn't start correctly

Distribution
----------------

See the `dist` directory. The distribution also requires `bin/build/lib` jars.

Change Log
----------

* 3.3:
  * Lightweight distributable
  * Changed CLI argument to require namespace
* 3.2:
  * Update requirement to Java 8
  * Changed CLI arguments to match .NET implementation
* 3.1: added in a new discovery service `hello`
* 3.0: added in CLI and web client query support
* 2.0: 
  * REST-compliant API
  * Bonjour registration
  * support for any MIME type
  * not compatible with 1.6.3
* 1.6.3: .NET only release with non-compliant REST endpoints
