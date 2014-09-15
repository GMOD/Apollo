
<%@ page import="org.bbop.apollo.Genome" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'genome.label', default: 'Genome')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-genome" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-genome" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list genome">
			
				<g:if test="${genomeInstance?.directory}">
				<li class="fieldcontain">
					<span id="directory-label" class="property-label"><g:message code="genome.directory.label" default="Directory" /></span>
					
						<span class="property-value" aria-labelledby="directory-label"><g:fieldValue bean="${genomeInstance}" field="directory"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${genomeInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="genome.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${genomeInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${genomeInstance?.tracks}">
				<li class="fieldcontain">
					<span id="tracks-label" class="property-label"><g:message code="genome.tracks.label" default="Tracks" /></span>
					
						<g:each in="${genomeInstance.tracks}" var="t">
						<span class="property-value" aria-labelledby="tracks-label"><g:link controller="track" action="show" id="${t.id}">${t?.name}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:genomeInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${genomeInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
