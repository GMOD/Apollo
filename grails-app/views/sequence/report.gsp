<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="report">
    <g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>


<g:render template="../layouts/reportHeader"/>

<div id="list-track" class="report-header content scaffold-list" role="main">
    <h3>Sequences for ${organism.commonName}</h3>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>
            <g:sortableColumn property="name" title="Name"/>
            <g:sortableColumn property="length" title="Length"/>
        </tr>
        </thead>
        <tbody>
        <g:each in="${sequenceInstanceList}" status="i" var="sequenceInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td>
                    <g:link action="show"
                            id="${sequenceInstance.id}">${fieldValue(bean: sequenceInstance, field: "name")}</g:link></td>
                <td>
                    ${sequenceInstance.length}
                %{--<g:link uri="">Browse</g:link>--}%
                </td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${sequenceInstanceCount ?: 0}"/>
    </div>
</div>

</body>
</html>
