<%@ page import="org.bbop.apollo.FeatureType" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'featureType.label', default: 'FeatureType')}"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<a href="#show-featureType" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                                  default="Skip to content&hellip;"/></a>

<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]"/></g:link></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="show-featureType" class="content scaffold-show" role="main">
    <h1><g:message code="default.show.label" args="[entityName]"/></h1>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <ol class="property-list featureType">

        <li class="fieldcontain">
            <span id="name-label" class="property-label"><g:message code="featureType.name.label"
                                                                    default="Name"/></span>

            <span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${featureTypeInstance}"
                                                                                    field="name"/></span>

        </li>

        <li class="fieldcontain">
            <span id="display-label" class="property-label"><g:message code="featureType.display.label"
                                                                       default="Display"/></span>
            <span class="property-value" aria-labelledby="display-label"><g:fieldValue bean="${featureTypeInstance}"
                                                                                       field="display"/></span>
        </li>

        <li class="fieldcontain">
            <span id="ontologyId-label" class="property-label"><g:message code="featureType.ontologyId.label"
                                                                          default="Ontology Id"/></span>
            <span class="property-value" aria-labelledby="ontologyId-label"><g:fieldValue bean="${featureTypeInstance}"
                                                                                          field="ontologyId"/></span>
        </li>

        <li class="fieldcontain">
            <span id="type-label" class="property-label"><g:message code="featureType.type.label"
                                                                    default="Type"/></span>
            <span class="property-value" aria-labelledby="type-label"><g:fieldValue bean="${featureTypeInstance}"
                                                                                    field="type"/></span>
        </li>

    </ol>
    <g:form url="[resource: featureTypeInstance, action: 'delete']" method="DELETE">
        <fieldset class="buttons">
            <g:link class="edit" action="edit" resource="${featureTypeInstance}"><g:message
                    code="default.button.edit.label" default="Edit"/></g:link>
            <g:actionSubmit class="delete" action="delete"
                            value="${message(code: 'default.button.delete.label', default: 'Delete')}"
                            onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');"/>
        </fieldset>
    </g:form>
</div>
</body>
</html>
