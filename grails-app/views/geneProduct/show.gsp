
<%@ page import="org.bbop.apollo.geneProduct.GeneProduct" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'geneProduct.label', default: 'GeneProduct')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-geneProduct" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-geneProduct" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list geneProduct">
			
				<g:if test="${geneProductInstance?.feature}">
				<li class="fieldcontain">
					<span id="feature-label" class="property-label"><g:message code="geneProduct.feature.label" default="Feature" /></span>
					
						<span class="property-value" aria-labelledby="feature-label"><g:link controller="feature" action="show" id="${geneProductInstance?.feature?.id}">${geneProductInstance?.feature?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.productName}">
				<li class="fieldcontain">
					<span id="productName-label" class="property-label"><g:message code="geneProduct.productName.label" default="Product Name" /></span>
					
						<span class="property-value" aria-labelledby="productName-label"><g:fieldValue bean="${geneProductInstance}" field="productName"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.reference}">
				<li class="fieldcontain">
					<span id="reference-label" class="property-label"><g:message code="geneProduct.reference.label" default="Reference" /></span>
					
						<span class="property-value" aria-labelledby="reference-label"><g:fieldValue bean="${geneProductInstance}" field="reference"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.dateCreated}">
				<li class="fieldcontain">
					<span id="dateCreated-label" class="property-label"><g:message code="geneProduct.dateCreated.label" default="Date Created" /></span>
					
						<span class="property-value" aria-labelledby="dateCreated-label"><g:formatDate date="${geneProductInstance?.dateCreated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.lastUpdated}">
				<li class="fieldcontain">
					<span id="lastUpdated-label" class="property-label"><g:message code="geneProduct.lastUpdated.label" default="Last Updated" /></span>
					
						<span class="property-value" aria-labelledby="lastUpdated-label"><g:formatDate date="${geneProductInstance?.lastUpdated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.evidenceRef}">
				<li class="fieldcontain">
					<span id="evidenceRef-label" class="property-label"><g:message code="geneProduct.evidenceRef.label" default="Evidence Ref" /></span>
					
						<span class="property-value" aria-labelledby="evidenceRef-label"><g:fieldValue bean="${geneProductInstance}" field="evidenceRef"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.evidenceRefLabel}">
				<li class="fieldcontain">
					<span id="evidenceRefLabel-label" class="property-label"><g:message code="geneProduct.evidenceRefLabel.label" default="Evidence Ref Label" /></span>
					
						<span class="property-value" aria-labelledby="evidenceRefLabel-label"><g:fieldValue bean="${geneProductInstance}" field="evidenceRefLabel"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.withOrFromArray}">
				<li class="fieldcontain">
					<span id="withOrFromArray-label" class="property-label"><g:message code="geneProduct.withOrFromArray.label" default="With Or From Array" /></span>
					
						<span class="property-value" aria-labelledby="withOrFromArray-label"><g:fieldValue bean="${geneProductInstance}" field="withOrFromArray"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.alternate}">
				<li class="fieldcontain">
					<span id="alternate-label" class="property-label"><g:message code="geneProduct.alternate.label" default="Alternate" /></span>
					
						<span class="property-value" aria-labelledby="alternate-label"><g:formatBoolean boolean="${geneProductInstance?.alternate}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.notesArray}">
				<li class="fieldcontain">
					<span id="notesArray-label" class="property-label"><g:message code="geneProduct.notesArray.label" default="Notes Array" /></span>
					
						<span class="property-value" aria-labelledby="notesArray-label"><g:fieldValue bean="${geneProductInstance}" field="notesArray"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneProductInstance?.owners}">
				<li class="fieldcontain">
					<span id="owners-label" class="property-label"><g:message code="geneProduct.owners.label" default="Owners" /></span>
					
						<g:each in="${geneProductInstance.owners}" var="o">
						<span class="property-value" aria-labelledby="owners-label"><g:link controller="user" action="show" id="${o.id}">${o?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:geneProductInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${geneProductInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
