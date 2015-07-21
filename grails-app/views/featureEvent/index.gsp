
<%@ page import="org.bbop.apollo.FeatureEvent" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'featureEvent.label', default: 'FeatureEvent')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-featureEvent" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-featureEvent" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<th><g:message code="featureEvent.editor.label" default="Editor" /></th>
					
						<g:sortableColumn property="originalJsonCommand" title="${message(code: 'featureEvent.originalJsonCommand.label', default: 'Original Json Command')}" />
					
						<g:sortableColumn property="newFeaturesJsonArray" title="${message(code: 'featureEvent.newFeaturesJsonArray.label', default: 'New Features Json Array')}" />
					
						<g:sortableColumn property="oldFeaturesJsonArray" title="${message(code: 'featureEvent.oldFeaturesJsonArray.label', default: 'Old Features Json Array')}" />
					
						<g:sortableColumn property="name" title="${message(code: 'featureEvent.name.label', default: 'Name')}" />
					
						<g:sortableColumn property="uniqueName" title="${message(code: 'featureEvent.uniqueName.label', default: 'Unique Name')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${featureEventInstanceList}" status="i" var="featureEventInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${featureEventInstance.id}">${fieldValue(bean: featureEventInstance, field: "editor")}</g:link></td>
					
						<td>${fieldValue(bean: featureEventInstance, field: "originalJsonCommand")}</td>
					
						<td>${fieldValue(bean: featureEventInstance, field: "newFeaturesJsonArray")}</td>
					
						<td>${fieldValue(bean: featureEventInstance, field: "oldFeaturesJsonArray")}</td>
					
						<td>${fieldValue(bean: featureEventInstance, field: "name")}</td>
					
						<td>${fieldValue(bean: featureEventInstance, field: "uniqueName")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${featureEventInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
