<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 4/13/15
  Time: 2:18 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="oldlook">
    <title>Apollo Version</title>
</head>

<body>

<nav class="navbar navbar-custom" role="navigation">
    <div class="navbar-header">
        <div class="container-fluid">
            <a class="navbar-brand" href="http://genomearchitect.org/" target="_blank">
                <img style="padding: 0;margin: -18px; height:53px;" id="logo" src="/apollo/images/ApolloLogo_100x36.png">
            </a>

        </div>
    </div>

</nav>

<div id="list-track" class="content scaffold-list" role="main">
    <h3>Apollo Genome Annotator
    %{--<g:meta name="app.version"/>--}%
    </h3>
    <ul>
        <li>Version: <a href='https://github.com/GMOD/Apollo'><g:meta name="app.version"/></a></li>
        <li>Grails version: <g:meta name="app.grails.version"/></li>
        <li>Groovy version: ${GroovySystem.getVersion()}</li>
        <li>JVM version: ${System.getProperty('java.version')}</li>
    </ul>

</div>

</body>
</html>