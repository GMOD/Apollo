<%@ page import="org.bbop.apollo.Organism" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'organism.label', default: 'Organism')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>
<a href="#list-organism" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                               default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="list-organism" class="content scaffold-list" role="main">
    <h1><g:message code="default.list.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>

            %{--<g:sortableColumn property="directory" title="${message(code: 'organism.directory.label', default: 'Directory')}" />--}%
            <g:sortableColumn property="commonName"
                              title="${message(code: 'organism.commonName.label', default: 'Common Name')}"/>

            <g:sortableColumn property="genus" title="${message(code: 'organism.genus.label', default: 'Genus')}"/>

            <th># Sequences</th>
            <th>Action</th>

            %{--<g:sortableColumn property="abbreviation" title="${message(code: 'organism.abbreviation.label', default: 'Abbreviation')}" />--}%

            %{--<g:sortableColumn property="comment" title="${message(code: 'organism.comment.label', default: 'Comment')}" />--}%


            %{--<g:sortableColumn property="species" title="${message(code: 'organism.species.label', default: 'Species')}" />--}%

        </tr>
        </thead>
        <tbody>
        <g:each in="${organismInstanceList}" status="i" var="organismInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                %{--<td><g:link action="show" id="${organismInstance.id}">${fieldValue(bean: organismInstance, field: "directory")}</g:link></td>--}%

                %{--<td><g:link action="show" id="${organismInstance.id}">${fieldValue(bean: organismInstance, field: "abbreviation")}</g:link></td>--}%

                %{--<td>${fieldValue(bean: organismInstance, field: "comment")}</td>--}%

                <td>${fieldValue(bean: organismInstance, field: "commonName")}
                ${organismInstance.abbreviation}
                </td>

                <td>${fieldValue(bean: organismInstance, field: "genus")} ${organismInstance.species}</td>

                <td>
                    ${organismInstance.sequences?.size()}
                </td>
                <td>
                    <g:link action="show" id="${organismInstance.id}">Details</g:link>
                    <g:link url="/jbrowse">Browse</g:link>
                </td>

                %{--<td>${fieldValue(bean: organismInstance, field: "species")}</td>--}%

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${organismInstanceCount ?: 0}"/>
    </div>
</div>
</body>
</html>
