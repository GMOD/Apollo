
<%@ page import="org.bbop.apollo.CannedKey" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'cannedKey.label', default: 'CannedKey')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-cannedKey" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-cannedKey" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="label" title="${message(code: 'cannedKey.label.label', default: 'Label')}" />
						<th>Feature Types</th>
						<th>Organisms</th>

						<g:sortableColumn property="metadata" title="${message(code: 'cannedKey.metadata.label', default: 'Metadata')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${cannedKeyInstanceList}" status="i" var="cannedKeyInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${cannedKeyInstance.id}">${fieldValue(bean: cannedKeyInstance, field: "label")}</g:link></td>

						<td>
							<g:each in="${cannedKeyInstance.featureTypes.sort() { a,b -> a.display <=> b.display }}" var="featureType">
								${featureType.type}:${featureType.name}
							</g:each>
						</td>
						<td>
							<g:each in="${organismFilters.get(cannedKeyInstance)}" var="filter">
								<g:link controller="organism" id="${filter.organism.id}">${filter.organism.commonName}</g:link>
							</g:each>
						</td>

						<td>${fieldValue(bean: cannedKeyInstance, field: "metadata")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${cannedKeyInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
