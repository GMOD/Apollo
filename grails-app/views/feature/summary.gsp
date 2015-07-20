<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
%{--<a href="#show-feature" class="skip" tabindex="-1"><g:message code="default.link.skip.label"--}%
%{--default="Skip to content&hellip;"/></a>--}%

<nav class="navbar navbar-default">
    <div class="container-fluid">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div class="navbar-header">
            %{--<a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a>--}%
            %{--<button type="button" class="navbar-toggle collapsed" data-toggle="collapse"--}%
            %{--data-target="#bs-example-navbar-collapse-1" aria-expanded="false">--}%
            %{--<span class="sr-only">Toggle navigation</span>--}%
            %{--<span class="icon-bar"></span>--}%
            %{--<span class="icon-bar"></span>--}%
            %{--<span class="icon-bar"></span>--}%
            %{--</button>--}%
            %{--<a class="navbar-brand glyphicon glyphicon-list-alt" href="${createLink(uri: '/')}">Reports</a>--}%
            <div class="input-prepend">
                <a class="navbar-brand glyphicon glyphicon-home" href="${createLink(uri: '/')}">Home</a>

                <div class="btn btn-group">
                    <button class="btn dropdown-toggle glyphicon glyphicon-list-alt " data-toggle="dropdown">
                        Reports
                        <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu">
                        <li>Organism Annotations</li>
                        %{--<li><a href="#">Organism Annotations</a></li>--}%
                    </ul>
                </div>
            </div>

        </div>
    </div>
</nav>

%{--<div class="nav" role="navigation">--}%
%{--<ul>--}%
%{--<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>--}%

%{--</ul>--}%
%{--<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>--}%
%{--<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>--}%
%{--<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>--}%
%{--</div>--}%

<div id="show-feature" class="content scaffold-show col-sm-offset-0 report-header" role="main">
    <h3>Summary</h3>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <g:render template="summaryEntry" model="[summaryData: featureSummaryInstance]"/>
    <g:each in="${featureSummaries}" var="featureSummaryInstance">
        <g:render template="summaryEntry"
                  model="[organism: featureSummaryInstance.key, summaryData: featureSummaryInstance.value]"/>
    </g:each>
</div>

</body>
</html>
