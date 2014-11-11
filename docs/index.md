Apollo
======

A genome annotation editor.  The stack is a Java web application / database backend and a Javascript client that runs in a web browser as a JBrowse plugin.  

For general information on WebApollo, go to: 
http://genomearchitect.org/

The WebApollo client is implemented as a plugin for JBrowse, for more information on JBrowse, please visit:
http://jbrowse.org

![Build status](https://travis-ci.org/GMOD/Apollo.svg?branch=master)

Introduction
------------

This guide will walk you through the server side installation for Web
Apollo. Web Apollo is a web-based application, so the only client side
requirement is a web browser. Note that Web Apollo has only been tested
on Chrome, Firefox, and Safari. It has not been tested with Internet
Explorer.

Installation
------------

You can download the latest Web Apollo release as a
[tarball](https://github.com/gmod/Apollo.git) or from
[genomearchitect.org] (not available for 1.x release branch yet).

All installation steps will be done through a shell. We'll be using Tomcat 7
as our servlet container and PostgreSQL to manage authentication data.

We'll use sample data from the Pythium ultimum genome, provided as a
[separate download](http://icebox.lbl.gov/webapollo/data/pyu_data.tgz).

Latest release: https://github.com/GMOD/Apollo/archive/1.0.0-RC2.tar.gz

