<%@ page import="grails.util.Metadata" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="oldlook">
    <title>About Apollo</title>
    <style type="text/css" media="screen">
    h2 {
        margin-top: 1em;
        margin-bottom: 0.3em;
        margin-left: 1.8em;
    }
    h3 {
        margin-top: 2em;
        margin-bottom: 0.3em;
        margin-left: 2.4em;
    }

    ul {
        margin-left: 2em;
        font-size: larger;
    }
        .big-annotator-button{
            margin-left: 50px;
            margin-top: 20px;
            padding: 20px;
            font-size: 50px;
            background-color: #396494;
            border: solid black 5px ;
            border-radius: 10px;
            width: 280px;
            color: white !important;
            font-weight: bolder;
        }

    </style>
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

<div class="big-annotator-button">
    <a href="../annotator/" target="_blank" style="color: white;">
        Annotate
    </a>
</div>

<div id="list-track" class="content scaffold-list" role="main">
    <h2>Apollo Genome Annotation Editor</h2>
    <ul>
        <li>
            Cite Apollo: <a
                href="https://doi.org/10.1371/journal.pcbi.1006790">Dunn, N. A. et al. Apollo: Democratizing genome annotation. PLoS Comput. Biol. 15, e1006790 (2019)</a>
        </li>
        <li>
            <a href="https://genomearchitect.readthedocs.io/en/latest/index.html">Latest Documentation</a>
        </li>
        <li>
            <a href="https://genomearchitect.readthedocs.io/en/latest/UsersGuide.html">User's Guide</a>
        </li>
        <li>
            <a href="https://github.com/gmod/apollo">Apollo Source</a>
        </li>
        <li>
            <a href="https://github.com/gmod/apollo/issues/new">Request Feature / Report Bug</a>
        </li>
        <li>
            <a href="https://groups.google.com/a/lbl.gov/forum/#!forum/apollo">User's Group</a> archive and signup.  <a
                href="mailto:apollo@lbl.gov">Email user's group directly</a>.
        </li>
    </ul>

    <hr/>
    <ul>
        <li>Version:
        %{--${grails.util.Metadata.current[attrs[app.version]]}--}%
            <g:if test="${grails.util.Metadata.current['app.version'].contains('SNAPSHOT')}">
                <a href='https://github.com/GMOD/Apollo/releases'><g:meta name="app.version"/></a>
            </g:if>
            <g:else>
                <a href='https://github.com/GMOD/Apollo/releases/tag/<g:meta name="app.version"/>'><g:meta
                        name="app.version"/></a>
            </g:else>
        </li>
        <li>Grails version: <g:meta name="app.grails.version"/></li>
        <li>Groovy version: ${GroovySystem.getVersion()}</li>
        <li>JVM version: ${System.getProperty('java.version')}</li>
        <li>Servlet Container Version: ${application.getServerInfo()}</li>
        <li>JBrowse config: ${grailsApplication.config.jbrowse.git.branch ? grailsApplication.config.jbrowse.git.branch : grailsApplication.config.jbrowse.git.tag}</li>
        <li>JBrowse url: ${grailsApplication.config.jbrowse.git.url}</li>
    </ul>

    <h3>JBrowse Plugins</h3>
    <ul>
        <g:each in="${grailsApplication.config.jbrowse.plugins}">
            <li>${it}</li>
        </g:each>
    </ul>

    <h3>Apollo Config</h3>
    <ul>
        <g:each in="${grailsApplication.config.apollo}">
            <g:if test="${!it.toString().toLowerCase().contains('password')}">
                <li>${it}</li>
            </g:if>
        </g:each>
    </ul>

</div>

</body>
</html>
