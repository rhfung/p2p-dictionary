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

* Java SE 1.6 or higher

* For auto-peer discovery:
  * Requires Apple Bonjour Print Services for Windows:
  http://support.apple.com/kb/DL999
  * DNS_SD.jar from Apple Bonjour (Windows)
  * Windows computer

* Java libraries (in the `lib` directory):
  * Jackson JSON 2.0 core libraries (annotations, core, databind)
  * Apache Common libraries (cli, fileupload, io, lang)

* Docker, for running locally.

Documentation
-------------

This library needs a lot more documentation. I have written documentation for parts of P2P Dictionary
in the `doc` directory.

Examples
--------

Taken from examples/first-example/TestRun.java:

    import com.rhfung.P2PDictionary.*;

    // ...

    P2PDictionary node = new P2PDictionary("test", 3333, "test", P2PDictionaryServerMode.AutoRegister, P2PDictionaryClientMode.AutoConnect, 1500);
    // ...
    node.put(Integer.toString(node.getLocalID()) + "/message0", "hello world");
    // ...
    node.close();

See other sample projects in `examples`

Running in Docker
-----------------

To run this project locally, run the following commands from your Docker terminal:

    ./start

And then visit the URL ```http://docker_url:8765``` where `docker_url` is `localhost` (linux)
or the docker machine URL (mac)

The Docker-related configuration files are:

* start
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

* 3.0: adding in daemon and client support
* 2.0.x: REST-compliant API, Bonjour registration, support for any MIME type, and stability bug fixes.
         Not compatible with 1.6.3. Cross-platform.
* 1.6.3: .NET only release with non-compliant REST endpoints
