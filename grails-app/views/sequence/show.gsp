<%@ page import="org.bbop.apollo.Sequence" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'sequence.label', default: 'Sequence')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<a href="#show-sequence" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                               default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]"/></g:link></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="show-sequence" class="content scaffold-show" role="main">
    <h1><g:message code="default.show.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <ol class="property-list sequence">

        %{--<g:if test="${sequenceInstance?.sequence}">--}%
        <li class="fieldcontain">
            <span id="sequence-label" class="property-label"><g:message code="sequence.sequence.label"
                                                                        default="Sequence"/></span>

            <span class="property-value" aria-labelledby="sequence-label"><g:link controller="organism" action="show"
                                                                                  id="${sequenceInstance?.id}">${sequenceInstance?.organism?.commonName}</g:link></span>

        </li>
        %{--</g:if>--}%

        <li class="fieldcontain">
            <span id="name-label" class="property-label"><g:message code="sequence.name.label" default="Name"/></span>

            <span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${sequenceInstance}"
                                                                                    field="name"/>
                <ul>
                    <g:each in="${sequenceInstance?.featureLocations.sort(){a,b -> a.fmin <=> b.fmin }}" var="fl">
                        <li>
                            <g:link action="show" controller="featureLocation" id="${fl.id}">
                                ${fl.fmin}-${fl.fmax}  ${fl.feature.cvTerm}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
                %{--${featureLocations}--}%


                %{--<g:link action="show" controller="jbrowse"--}%
            </span>

        </li>


        <g:if test="${sequenceInstance?.users}">
            <li class="fieldcontain">
                <span id="users-label" class="property-label"><g:message code="sequence.users.label"
                                                                         default="Users"/></span>

                <g:each in="${sequenceInstance.users}" var="u">
                    <span class="property-value" aria-labelledby="users-label"><g:link controller="user" action="show"
                                                                                       id="${u.id}">${u?.username}</g:link></span>
                </g:each>

            </li>
        </g:if>

    </ol>
    <g:form url="[resource: sequenceInstance, action: 'delete']" method="DELETE">
        <fieldset class="buttons">
            <g:link class="edit" action="edit" resource="${sequenceInstance}"><g:message
                    code="default.button.edit.label" default="Edit"/></g:link>
            <g:actionSubmit class="delete" action="delete"
                            value="${message(code: 'default.button.delete.label', default: 'Delete')}"
                            onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>
        </fieldset>
    </g:form>
</div>
</body>
</html>
