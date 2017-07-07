
<%@ page import="org.bbop.apollo.AvailableStatus" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'availableStatus.label', default: 'AvailableStatus')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-availableStatus" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-availableStatus" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="value" title="${message(code: 'availableStatus.value.label', default: 'Value')}" />
						<th>Feature Types</th>
						<th>Organisms</th>

					</tr>
				</thead>
				<tbody>
				<g:each in="${availableStatusInstanceList}" status="i" var="availableStatusInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${availableStatusInstance.id}">${fieldValue(bean: availableStatusInstance, field: "value")}</g:link></td>

						<td>
							<g:each in="${availableStatusInstance.featureTypes.sort() { a,b -> a.display <=> b.display }}" var="featureType">
								${featureType.type}:${featureType.name}
							</g:each>
						</td>
						<td>
							<g:each in="${organismFilters.get(availableStatusInstance)}" var="filter">
								<g:link controller="organism" id="${filter.organism.id}">${filter.organism.commonName}</g:link>
							</g:each>
						</td>
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${availableStatusInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
