
<%@ page import="org.bbop.apollo.GeneProductName" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'geneProductName.label', default: 'GeneProductName')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-geneProductName" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-geneProductName" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list geneProductName">
			
				<g:if test="${geneProductNameInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="geneProductName.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${geneProductNameInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductNameInstance?.metadata}">
				<li class="fieldcontain">
					<span id="metadata-label" class="property-label"><g:message code="geneProductName.metadata.label" default="Metadata" /></span>
					
						<span class="property-value" aria-labelledby="metadata-label"><g:fieldValue bean="${geneProductNameInstance}" field="metadata"/></span>
					
				</li>
				</g:if>

				<g:if test="${geneProductNameInstance?.featureTypes}">
					<li class="fieldcontain">
						<span id="featureTypes-label" class="property-label"><g:message code="geneProductName.featureTypes.label"
																						default="Feature Types"/></span>

						<g:each in="${geneProductNameInstance.featureTypes}" var="f">
							<span class="property-value" aria-labelledby="featureTypes-label"><g:link controller="featureType"
																									  action="show"
																									  id="${f.id}">${f?.name}</g:link></span>
						</g:each>

					</li>
				</g:if>

				<g:if test="${organismFilters}">
					<li class="fieldcontain">
						<span id="organisms-label" class="property-label"><g:message code="geneProductName.organisms.label"
																					 default="Organisms"/></span>

						<g:each in="${organismFilters}" var="f">
							<span class="property-value" aria-labelledby="organisms-label"><g:link controller="organism"
																								   action="show"
																								   id="${f.id}">${f?.organism.commonName}</g:link></span>
						</g:each>
					</li>
				</g:if>

			</ol>
			<g:form url="[resource:geneProductNameInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${geneProductNameInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
