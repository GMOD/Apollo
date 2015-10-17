
<%@ page import="org.bbop.apollo.Proxy" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'proxy.label', default: 'Proxy')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-proxy" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-proxy" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="referenceUrl" title="${message(code: 'proxy.referenceUrl.label', default: 'Reference Url')}" />
					
						<g:sortableColumn property="targetUrl" title="${message(code: 'proxy.targetUrl.label', default: 'Target Url')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${proxyInstanceList}" status="i" var="proxyInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${proxyInstance.id}">${fieldValue(bean: proxyInstance, field: "referenceUrl")}</g:link></td>
					
						<td>${fieldValue(bean: proxyInstance, field: "targetUrl")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${proxyInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
