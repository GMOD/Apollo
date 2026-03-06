
<%@ page import="org.bbop.apollo.Proxy" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'proxy.label', default: 'Proxy')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-proxy" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-proxy" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list proxy">

				<li class="fieldcontain">
					<span id="referenceUrl-label" class="property-label"><g:message code="proxy.referenceUrl.label" default="Reference Url" /></span>

					<span class="property-value" aria-labelledby="referenceUrl-label"><g:fieldValue bean="${proxyInstance}" field="referenceUrl"/></span>

				</li>
				<li class="fieldcontain">
					<span id="targetUrl-label" class="property-label"><g:message code="proxy.targetUrl.label" default="Target Url" /></span>

					<span class="property-value" aria-labelledby="targetUrl-label"><g:fieldValue bean="${proxyInstance}" field="targetUrl"/></span>

				</li>

				<li class="fieldcontain">
					<span id="active-label" class="property-label"><g:message code="proxy.active.label" default="Active" /></span>

					<span class="property-value" aria-labelledby="active-label"><g:formatBoolean boolean="${proxyInstance?.active}" /></span>

				</li>

				%{--<g:if test="${proxyInstance?.fallbackOrder}">--}%
				<li class="fieldcontain">
					<span id="fallbackOrder-label" class="property-label"><g:message code="proxy.fallbackOrder.label" default="Fallback Order" /></span>
						<span class="property-value" aria-labelledby="fallbackOrder-label"><g:fieldValue bean="${proxyInstance}" field="fallbackOrder"/></span>
					
				</li>
				%{--</g:if>--}%
			
				%{--<g:if test="${proxyInstance?.lastSuccess}">--}%
				%{--<li class="fieldcontain">--}%
					%{--<span id="lastSuccess-label" class="property-label"><g:message code="proxy.lastSuccess.label" default="Last Success" /></span>--}%
					%{----}%
						%{--<span class="property-value" aria-labelledby="lastSuccess-label"><g:formatDate date="${proxyInstance?.lastSuccess}" /></span>--}%
					%{----}%
				%{--</li>--}%
				%{--</g:if>--}%
			
				%{--<g:if test="${proxyInstance?.lastFail}">--}%
				%{--<li class="fieldcontain">--}%
					%{--<span id="lastFail-label" class="property-label"><g:message code="proxy.lastFail.label" default="Last Fail" /></span>--}%
					%{----}%
						%{--<span class="property-value" aria-labelledby="lastFail-label"><g:formatDate date="${proxyInstance?.lastFail}" /></span>--}%
					%{----}%
				%{--</li>--}%
				%{--</g:if>--}%
			

			</ol>
			<g:form url="[resource:proxyInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${proxyInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
