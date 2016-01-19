<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-feature" class="content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>

    <g:form id="customform" name="myForm" url="[action:'changes',controller:'featureEvent']">
        <label for="ownerName">Owner:</label>
        <g:textField name="ownerName" maxlength="50" value="${ownerName}"/><br />
        <label for="featureType">Feature type:</label>
        <g:textField name="featureType" maxlength="50" value="${featureType}"/> <br />
        <label for="organismName">Organism:</label>
        <g:textField name="organismName" maxlength="50" value="${organismName}"/><br />

        <input type="submit" value="Submit" />
    </g:form>


    <table>
        <thead>
        <tr>
            <g:sortableColumn property="lastUpdated" title="Last updated"/>
            <g:sortableColumn property="organism" title="Organism"/>
            <g:sortableColumn property="sequencename" title="Sequence name"/>
            <g:sortableColumn property="name" title="Name"/>
            <g:sortableColumn property="owners" title="Owner"/>
            <g:sortableColumn property="cvTerm" title="Feature type"/>
        </tr>
        </thead>
        <tbody>
        <g:each in="${features}" status="i" var="feature">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                <td>
                    <g:formatDate format="E dd-MMM-yy" date="${feature.lastUpdated}"/>
                </td>
                <td>
                    ${feature.featureLocation.sequence.organism.commonName}
                </td>
                <td>
                    ${feature.featureLocation.sequence.name}
                </td>
                <td>
                    <g:link target="_blank" controller="annotator" action="loadLink" params="[loc: feature.featureLocation.sequence.name+':'+feature.featureLocation.fmin+'..'+feature.featureLocation.fmax, organism: feature.featureLocation.sequence.organism.id]">
                        ${feature.name}
                    </g:link>
                </td>

                <td>
                    ${feature.owner?.username}
                </td>
                <td>
                    ${feature.cvTerm}
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${featureCount ?: 0}" params="${params}"/>
    </div>
</div>
</body>
</html>
