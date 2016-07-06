
<%@ page import="org.bbop.apollo.FeatureType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'featureType.label', default: 'FeatureType')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-featureType" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-featureType" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="name" title="${message(code: 'featureType.name.label', default: 'Name')}" />

						<g:sortableColumn property="display" title="${message(code: 'featureType.display.label', default: 'Display')}" />

						<g:sortableColumn property="ontologyId" title="${message(code: 'featureType.ontologyId.label', default: 'Ontology Id')}" />

						<g:sortableColumn property="type" title="${message(code: 'featureType.type.label', default: 'Type')}" />
						<th>Filter</th>
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${featureTypeInstanceList}" status="i" var="featureTypeInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${featureTypeInstance.id}">${fieldValue(bean: featureTypeInstance, field: "display")}</g:link></td>

						<td>${fieldValue(bean: featureTypeInstance, field: "display")}</td>
						<td>${fieldValue(bean: featureTypeInstance, field: "ontologyId")}</td>
						<td>${fieldValue(bean: featureTypeInstance, field: "type")}</td>

						<td>${fieldValue(bean: featureTypeInstance, field: "type")}:${featureTypeInstance.name}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${featureTypeInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
