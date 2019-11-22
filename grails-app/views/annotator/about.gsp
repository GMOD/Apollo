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

  ul {
    margin-left: 2em;
    font-size: larger;
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

<div id="list-track" class="content scaffold-list" role="main">
  <h2>Apollo Genome Annotation Editor</h2>
  <ul>
  <li>
    Cite Apollo: <a href="https://doi.org/10.1371/journal.pcbi.1006790">Dunn, N. A. et al. Apollo: Democratizing genome annotation. PLoS Comput. Biol. 15, e1006790 (2019)</a>
  </li>
  <li>
    <a href="https://genomearchitect.readthedocs.io/en/latest/index.html">Latest Documentation</a>
  </li>
  <li>
    <a href="https://genomearchitect.readthedocs.io/en/latest/UsersGuide.html">User's Guide</a>
  </li>
  <li>
    <a href="https://github.com/gmod/apollo">Apollo Source and Issue Reporting</a>
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
