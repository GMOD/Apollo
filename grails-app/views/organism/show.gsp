
<%@ page import="org.bbop.apollo.Organism" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'organism.label', default: 'Organism')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-organism" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-organism" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list organism">
			
				<g:if test="${organismInstance?.directory}">
				<li class="fieldcontain">
					<span id="directory-label" class="property-label"><g:message code="organism.directory.label" default="Directory" /></span>
					
						<span class="property-value" aria-labelledby="directory-label"><g:fieldValue bean="${organismInstance}" field="directory"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.abbreviation}">
				<li class="fieldcontain">
					<span id="abbreviation-label" class="property-label"><g:message code="organism.abbreviation.label" default="Abbreviation" /></span>
					
						<span class="property-value" aria-labelledby="abbreviation-label"><g:fieldValue bean="${organismInstance}" field="abbreviation"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.comment}">
				<li class="fieldcontain">
					<span id="comment-label" class="property-label"><g:message code="organism.comment.label" default="Comment" /></span>
					
						<span class="property-value" aria-labelledby="comment-label"><g:fieldValue bean="${organismInstance}" field="comment"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.commonName}">
				<li class="fieldcontain">
					<span id="commonName-label" class="property-label"><g:message code="organism.commonName.label" default="Common Name" /></span>
					
						<span class="property-value" aria-labelledby="commonName-label"><g:fieldValue bean="${organismInstance}" field="commonName"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.genus}">
				<li class="fieldcontain">
					<span id="genus-label" class="property-label"><g:message code="organism.genus.label" default="Genus" /></span>
					
						<span class="property-value" aria-labelledby="genus-label"><g:fieldValue bean="${organismInstance}" field="genus"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.organismDBXrefs}">
				<li class="fieldcontain">
					<span id="organismDBXrefs-label" class="property-label"><g:message code="organism.organismDBXrefs.label" default="Organism DBX refs" /></span>
					
						<g:each in="${organismInstance.organismDBXrefs}" var="o">
						<span class="property-value" aria-labelledby="organismDBXrefs-label"><g:link controller="organismDBXref" action="show" id="${o.id}">${o?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.organismProperties}">
				<li class="fieldcontain">
					<span id="organismProperties-label" class="property-label"><g:message code="organism.organismProperties.label" default="Organism Properties" /></span>
					
						<g:each in="${organismInstance.organismProperties}" var="o">
						<span class="property-value" aria-labelledby="organismProperties-label"><g:link controller="organismProperty" action="show" id="${o.id}">${o?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.sequences}">
				<li class="fieldcontain">
					<span id="sequences-label" class="property-label"><g:message code="organism.sequences.label" default="Sequences" /></span>
					
						<g:each in="${organismInstance.sequences}" var="s">
						<span class="property-value" aria-labelledby="sequences-label"><g:link controller="sequence" action="show" id="${s.id}">${s?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${organismInstance?.species}">
				<li class="fieldcontain">
					<span id="species-label" class="property-label"><g:message code="organism.species.label" default="Species" /></span>
					
						<span class="property-value" aria-labelledby="species-label"><g:fieldValue bean="${organismInstance}" field="species"/></span>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:organismInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${organismInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
