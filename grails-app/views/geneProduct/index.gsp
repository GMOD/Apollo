
<%@ page import="org.bbop.apollo.geneProduct.GeneProduct" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'geneProduct.label', default: 'GeneProduct')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-geneProduct" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-geneProduct" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<th><g:message code="geneProduct.feature.label" default="Feature" /></th>
					
						<g:sortableColumn property="productName" title="${message(code: 'geneProduct.productName.label', default: 'Product Name')}" />
					
						<g:sortableColumn property="reference" title="${message(code: 'geneProduct.reference.label', default: 'Reference')}" />
					
						<g:sortableColumn property="dateCreated" title="${message(code: 'geneProduct.dateCreated.label', default: 'Date Created')}" />
					
						<g:sortableColumn property="lastUpdated" title="${message(code: 'geneProduct.lastUpdated.label', default: 'Last Updated')}" />
					
						<g:sortableColumn property="evidenceRef" title="${message(code: 'geneProduct.evidenceRef.label', default: 'Evidence Ref')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${geneProductInstanceList}" status="i" var="geneProductInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${geneProductInstance.id}">${fieldValue(bean: geneProductInstance, field: "feature")}</g:link></td>
					
						<td>${fieldValue(bean: geneProductInstance, field: "productName")}</td>
					
						<td>${fieldValue(bean: geneProductInstance, field: "reference")}</td>
					
						<td><g:formatDate date="${geneProductInstance.dateCreated}" /></td>
					
						<td><g:formatDate date="${geneProductInstance.lastUpdated}" /></td>
					
						<td>${fieldValue(bean: geneProductInstance, field: "evidenceRef")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${geneProductInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
