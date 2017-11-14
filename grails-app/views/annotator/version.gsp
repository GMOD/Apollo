<%--
  Created by IntelliJ IDEA.
  User: ndunn
  Date: 4/13/15
  Time: 2:18 PM
--%>

<%@ page import="grails.util.Metadata" contentType="text/html;charset=UTF-8" %>
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
                <g:img style="padding: 0;margin: -18px; height:53px;" id="logo" file="ApolloLogo_100x36.png"/>
            </a>
        </div>
    </div>

</nav>

<div id="list-track" class="content scaffold-list" role="main">
    <h3>Apollo Genome Annotator</h3>
    <ul>
        <li>Version:
            %{--${grails.util.Metadata.current[attrs[app.version]]}--}%
            <g:if test="${grails.util.Metadata.current['app.version'].contains('SNAPSHOT')}">
               <a href='https://github.com/GMOD/Apollo/releases'><g:meta name="app.version"/></a>
            </g:if>
            <g:else>
                <a href='https://github.com/GMOD/Apollo/releases/tag/<g:meta name="app.version"/>'><g:meta name="app.version"/></a>
            </g:else>
        </li>
        <li>Grails version: <g:meta name="app.grails.version"/></li>
        <li>Groovy version: ${GroovySystem.getVersion()}</li>
        <li>JVM version: ${System.getProperty('java.version')}</li>
        <li>Servlet Container Version: ${application.getServerInfo()}</li>
    </ul>

</div>

</body>
</html>