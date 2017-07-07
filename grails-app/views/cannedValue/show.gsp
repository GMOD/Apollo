<%@ page import="org.bbop.apollo.CannedValue" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'cannedValue.label', default: 'CannedValue')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<a href="#show-cannedValue" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                                  default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]"/></g:link></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="show-cannedValue" class="content scaffold-show" role="main">
    <h1><g:message code="default.show.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <ol class="property-list cannedValue">

        <g:if test="${cannedValueInstance?.label}">
            <li class="fieldcontain">
                <span id="label-label" class="property-label"><g:message code="cannedValue.label.label"
                                                                         default="Label"/></span>

                <span class="property-value" aria-labelledby="label-label"><g:fieldValue bean="${cannedValueInstance}"
                                                                                         field="label"/></span>

            </li>
        </g:if>

        <g:if test="${cannedValueInstance?.metadata}">
            <li class="fieldcontain">
                <span id="metadata-label" class="property-label"><g:message code="cannedValue.metadata.label"
                                                                            default="Metadata"/></span>

                <span class="property-value" aria-labelledby="metadata-label"><g:fieldValue
                        bean="${cannedValueInstance}" field="metadata"/></span>

            </li>
        </g:if>

        <g:if test="${cannedValueInstance?.featureTypes}">
            <li class="fieldcontain">
                <span id="featureTypes-label" class="property-label"><g:message code="cannedValue.featureTypes.label"
                                                                                default="Feature Types"/></span>

                <g:each in="${cannedValueInstance.featureTypes}" var="f">
                    <span class="property-value" aria-labelledby="featureTypes-label"><g:link controller="featureType"
                                                                                              action="show"
                                                                                              id="${f.id}">${f?.name}</g:link></span>
                </g:each>

            </li>
        </g:if>

        <g:if test="${organismFilters}">
            <li class="fieldcontain">
                <span id="organisms-label" class="property-label"><g:message code="cannedValue.organisms.label" default="Organisms" /></span>

                <g:each in="${organismFilters}" var="f">
                    <span class="property-value" aria-labelledby="organisms-label"><g:link controller="organism" action="show" id="${f.id}">${f?.organism.commonName}</g:link></span>
                </g:each>
            </li>
        </g:if>

    </ol>
    <g:form url="[resource: cannedValueInstance, action: 'delete']" method="DELETE">
        <fieldset class="buttons">
            <g:link class="edit" action="edit" resource="${cannedValueInstance}"><g:message
                    code="default.button.edit.label" default="Edit"/></g:link>
            <g:actionSubmit class="delete" action="delete"
                            value="${message(code: 'default.button.delete.label', default: 'Delete')}"
                            onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>
        </fieldset>
    </g:form>
</div>
</body>
</html>
