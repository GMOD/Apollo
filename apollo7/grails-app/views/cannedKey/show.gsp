<%@ page import="org.bbop.apollo.CannedKey" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'cannedKey.label', default: 'CannedKey')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<a href="#show-cannedKey" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                                default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]"/></g:link></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="show-cannedKey" class="content scaffold-show" role="main">
    <h1><g:message code="default.show.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <ol class="property-list cannedKey">

        <g:if test="${cannedKeyInstance?.label}">
            <li class="fieldcontain">
                <span id="label-label" class="property-label"><g:message code="cannedKey.label.label"
                                                                         default="Label"/></span>

                <span class="property-value" aria-labelledby="label-label"><g:fieldValue bean="${cannedKeyInstance}"
                                                                                         field="label"/></span>

            </li>
        </g:if>

        <g:if test="${cannedKeyInstance?.metadata}">
            <li class="fieldcontain">
                <span id="metadata-label" class="property-label"><g:message code="cannedKey.metadata.label"
                                                                            default="Metadata"/></span>

                <span class="property-value" aria-labelledby="metadata-label"><g:fieldValue bean="${cannedKeyInstance}"
                                                                                            field="metadata"/></span>

            </li>
        </g:if>

        <g:if test="${cannedKeyInstance?.featureTypes}">
            <li class="fieldcontain">
                <span id="featureTypes-label" class="property-label"><g:message code="cannedKey.featureTypes.label"
                                                                                default="Feature Types"/></span>

                <g:each in="${cannedKeyInstance.featureTypes}" var="f">
                    <span class="property-value" aria-labelledby="featureTypes-label"><g:link controller="featureType"
                                                                                              action="show"
                                                                                              id="${f.id}">${f?.display}</g:link></span>
                </g:each>

            </li>
        </g:if>

        <g:if test="${organismFilters}">
            <li class="fieldcontain">
                <span id="organisms-label" class="property-label"><g:message code="cannedKey.organisms.label"
                                                                             default="Organisms"/></span>

                <g:each in="${organismFilters}" var="f">
                    <span class="property-value" aria-labelledby="organisms-label"><g:link controller="organism"
                                                                                           action="show"
                                                                                           id="${f.id}">${f?.organism.commonName}</g:link></span>
                </g:each>
            </li>
        </g:if>

    %{--<g:if test="${cannedKeyInstance?.values}">--}%
    %{--<li class="fieldcontain">--}%
    %{--<span id="values-label" class="property-label"><g:message code="cannedKey.values.label" default="Values" /></span>--}%
    %{----}%
    %{--<g:each in="${cannedKeyInstance.values}" var="v">--}%
    %{--<span class="property-value" aria-labelledby="values-label"><g:link controller="cannedValue" action="show" id="${v.id}">${v?.label}</g:link></span>--}%
    %{--</g:each>--}%
    %{----}%
    %{--</li>--}%
    %{--</g:if>--}%

    </ol>
    <g:form url="[resource: cannedKeyInstance, action: 'delete']" method="DELETE">
        <fieldset class="buttons">
            <g:link class="edit" action="edit" resource="${cannedKeyInstance}"><g:message
                    code="default.button.edit.label" default="Edit"/></g:link>
            <g:actionSubmit class="delete" action="delete"
                            value="${message(code: 'default.button.delete.label', default: 'Delete')}"
                            onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>
        </fieldset>
    </g:form>
</div>
</body>
</html>
