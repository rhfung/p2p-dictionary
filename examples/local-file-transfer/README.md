LAN File Transfer
=================

LAN File Transfer is a web application that allows a person to transfer files
on a personal LAN without using the cloud. It is Dropbox without the cloud.

Copyright (C) Richard H Fung, 2012
You agree to the LICENSE before using this Software.

Requirements
------------

* P2P-dict-2.1 (Java library only)
* Java 1.6 or higher (for server)
* Modern web browser (tested Chrome and Firefox)

Implementation
--------------
This demo project works because P2P Dictionary implements its REST API over the
HTTP protocol, which allows it to behave like a normal web server. This project 
"shares" dictionary keys that correspond to web pages. The web pages implement
code to upload content via the POST method to the dictionary's REST endpoint.
