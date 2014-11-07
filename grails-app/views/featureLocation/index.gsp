
<%@ page import="org.bbop.apollo.FeatureLocation" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'featureLocation.label', default: 'FeatureLocation')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-featureLocation" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-featureLocation" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<th><g:message code="featureLocation.feature.label" default="Feature" /></th>
					
						<g:sortableColumn property="fmin" title="${message(code: 'featureLocation.fmin.label', default: 'Fmin')}" />
					
						<g:sortableColumn property="fmax" title="${message(code: 'featureLocation.fmax.label', default: 'Fmax')}" />
					
						<g:sortableColumn property="isFminPartial" title="${message(code: 'featureLocation.isFminPartial.label', default: 'Is Fmin Partial')}" />
					
						<th><g:message code="featureLocation.sourceFeature.label" default="Source Feature" /></th>
					
						<g:sortableColumn property="isFmaxPartial" title="${message(code: 'featureLocation.isFmaxPartial.label', default: 'Is Fmax Partial')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${featureLocationInstanceList}" status="i" var="featureLocationInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${featureLocationInstance.id}">${fieldValue(bean: featureLocationInstance, field: "feature")}</g:link></td>
					
						<td>${fieldValue(bean: featureLocationInstance, field: "fmin")}</td>
					
						<td>${fieldValue(bean: featureLocationInstance, field: "fmax")}</td>
					
						<td><g:formatBoolean boolean="${featureLocationInstance.isFminPartial}" /></td>
					
						<td>${fieldValue(bean: featureLocationInstance, field: "sourceFeature")}</td>
					
						<td><g:formatBoolean boolean="${featureLocationInstance.isFmaxPartial}" /></td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${featureLocationInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
