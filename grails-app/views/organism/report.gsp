<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <title>Organism Report</title>
</head>

<body>
%{--<a href="#show-feature" class="skip" tabindex="-1"><g:message code="default.link.skip.label"--}%
%{--default="Skip to content&hellip;"/></a>--}%

<g:render template="../layouts/reportHeader"/>

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
    <g:if test="${isSuperAdmin}">
        <g:render template="summaryEntry" model="[summaryData: organismSummaryInstance]"/>
    </g:if>
    <g:each in="${organismSummaries}" var="organismSummaryInstance">
        <g:render template="summaryEntry"
                  model="[organism: organismSummaryInstance.key, summaryData: organismSummaryInstance.value]"/>
    </g:each>
</div>

</body>
</html>
