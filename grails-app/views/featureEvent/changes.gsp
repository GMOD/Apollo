<%@ page import="org.bbop.apollo.FeatureEvent" %>
<%@ page import="org.bbop.apollo.history.FeatureEventView" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'featureEvent.label', default: 'FeatureEvent')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-featureEvent" class="content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>

            %{--<th><g:message code="featureEvent.editor.label" default="Editor" /></th>--}%
            <g:sortableColumn property="dateCreated" title="Date"/>
            <g:sortableColumn property="current" title="Current"/>
            <g:sortableColumn property="name" title="Name"/>
            <g:sortableColumn property="editor.username" title="Editor"/>
            <th>Type</th>
            <g:sortableColumn property="operation" title="Operation"/>

        </tr>
        </thead>
        <tbody>
        <g:each in="${featureEventViewList}" status="i" var="featureEventInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    <g:formatDate format="E dd-MMM-yy" date="${featureEventInstance.featureEvent.dateCreated}"/>
                </td>
                <td>
                    <g:if test="${featureEventInstance.featureEvent.current }">
                        <span class="glyphicon glyphicon-check" style="color:green;"></span>
                    </g:if>
                </td>
                <td>${featureEventInstance.featureEvent.name}</td>

                <td>
                    <g:link action="detail" controller="annotator" id="${featureEventInstance.featureEvent.editor.id}">
                        ${featureEventInstance.featureEvent.editor.username}
                    </g:link>
                </td>
                <td>
                    ${featureEventInstance.feature.cvTerm}
                </td>

                <td>${featureEventInstance.featureEvent.operation}</td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${featureEventInstanceCount ?: 0}"/>
    </div>
</div>
</body>
</html>
