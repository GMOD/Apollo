Introduction
------------
 
This guide will walk you through the server side installation for Web
Apollo. Web Apollo is a web-based application, so the only client side
requirement is a web browser. Note that Web Apollo has only been tested
on Chrome, Firefox, and Safari. It has not been tested with Internet
Explorer.
 
Installation
------------
Let's get started!
 
You can download the latest Web Apollo release from [github](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org](http://genomearchitect.org) (the 1.x release branch is not available from genomearchitect yet).

All installation steps will be done through a shell. We'll be using Tomcat 7
as our servlet container and PostgreSQL to manage authentication data.

We'll use sample data from the Pythium ultimum genome, provided as a
[separate download](http://icebox.lbl.gov/webapollo/data/pyu_data.tgz).

To get started, download the 1.0 pre-release release candidate: [https://github.com/GMOD/Apollo/archive/1.0.0-RC2.tar.gz](https://github.com/GMOD/Apollo/archive/1.0.0-RC2.tar.gz)

