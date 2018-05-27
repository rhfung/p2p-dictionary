P2P Dictionary
==============

P2P Dictionary is a distributed key-value store for multiple computers on a local area network.
Each computer runs a P2P server, which replicates a subset of stored dictionary entries (key-value pairs).
Each computer chooses a subset of keys to subscribe to. This dictionary provides an API written for .NET
and Java applications. A REST interface is provided by the P2P server for read-only access to dictionary entries.
A local area network is defined by Apple Bonjour's local service discovery. Similar to other NoSQL implementations,
it does not provide an SQL interface or guarantee ACID (atomicity, consistency, isolation, durability).

Copyright (C) 2011-2016, Richard H Fung

License
-------

You agree to the LICENSE before using this Software.

Basic requirements
------------------

* Java SE 1.7 or higher OR Docker

* For Bonjour discovery on Windows:
  * Apple Bonjour Print Services for Windows: http://support.apple.com/kb/DL999
  * DNS_SD.jar from Apple Bonjour (Windows)
  * Windows computer

* For Linux-compatible Bonjour discovery:
  * jmdns.jar

* Java libraries (in the `lib` directory):
  * Jackson JSON 2.0 core libraries (annotations, core, databind)
  * Apache Common libraries (cli, fileupload, io, lang)

Documentation
-------------

This library needs a lot more documentation. I have written documentation for parts of P2P Dictionary
in the `doc` directory.

Examples
--------

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

Running Locally
---------------

If you have Java 1.7 already installed in your system path, you can run using:

    ./start [parameters]

By default it runs on ```http://localhost:8765```.

Running in Docker
-----------------

To run this project locally, run the following commands from your Docker terminal:

    ./start_docker [parameters]

And then visit the URL ```http://docker_url:8765``` where `docker_url` is `localhost` (linux)
or the docker machine URL (mac)

P2PD Parameters
---------------

Parameters:

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

The Docker-related configuration files are:

* start_docker
* .dockerignore
* Dockerfile
* docker/dockerstart

Related Projects
----------------

I forked this project from a graduate-level course on peer to peer networking.
I moved the original implementation, written in C# for the .NET platform,
into [another repository](https://github.com/rhfung/p2p-dictionary-csharp).

Distribution
----------------

See the `dist` directory. The distribution also requires `lib` jars.

### Change Log

* 3.1: added in a new discovery service
* 3.0: added in daemon and web client query support
* 2.0.x: REST-compliant API, Bonjour registration, support for any MIME type, and stability bug fixes.
         Not compatible with 1.6.3. Cross-platform.
* 1.6.3: .NET only release with non-compliant REST endpoints
