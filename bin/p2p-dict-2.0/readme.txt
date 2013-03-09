P2P Dictionary
Version 2.0.1

P2P Dictionary is a distributed key-value store for multiple computers on a local area network. Each computer runs a P2P server, which replicates a subset of stored dictionary entries (key-value pairs). Each computer chooses a subset of keys to subscribe to. This dictionary provides an API written for .NET and Java applications. A REST interface is provided by the P2P server for read-only access to dictionary entries. A local area network is defined by Apple Bonjour's local service discovery. Similar to other NoSQL implementations, it does not provide an SQL interface or guarantee ACID (atomicity, consistency, isolation, durability).

Copyright (C) 2011-2012, Richard H Fung

You agree to the LICENSE before using this Software.

Basic requirements:

* Requires Apple Bonjour Print Services for Windows:
  http://support.apple.com/kb/DL999

Requirements for Windows:
* Microsoft .NET Framework 4.0 on Windows PC
* Custom build of Mono.Zeroconf

Requirements for Java:
* Java SE 1.6
* Jackson JSON 2.0 core libraries (annotations, core, databind)
* DNS_SD.jar from Apple Bonjour

Documentation:

* API: http://www.rhfung.com/core/Projects/P2PDictionaryDocumentation
* Protocol: http://www.rhfung.com/core/Projects/RESTProtocolDocumentation

Changes:

2.0.x - new REST API, Bonjour registration, support for any MIME type, 
        and stability bug fixes. Not compatible with 1.6.3.
