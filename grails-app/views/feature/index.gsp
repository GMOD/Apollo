
<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-feature" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-feature" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="name" title="${message(code: 'feature.name.label', default: 'Name')}" />
					
						%{--<g:sortableColumn property="uniqueName" title="${message(code: 'feature.uniqueName.label', default: 'Unique Name')}" />--}%
					%{----}%
						%{--<th><g:message code="feature.dbxref.label" default="Dbxref" /></th>--}%
					
						<g:sortableColumn property="sequenceLength" title="${message(code: 'feature.sequenceLength.label', default: 'Sequence Length')}" />
					
						%{--<g:sortableColumn property="md5checksum" title="${message(code: 'feature.md5checksum.label', default: 'Md5checksum')}" />--}%
					
						%{--<g:sortableColumn property="isAnalysis" title="${message(code: 'feature.isAnalysis.label', default: 'Is Analysis')}" />--}%
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${featureInstanceList}" status="i" var="featureInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${featureInstance.id}">${fieldValue(bean: featureInstance, field: "name")}</g:link></td>
					
						%{--<td>${fieldValue(bean: featureInstance, field: "uniqueName")}</td>--}%
					
						%{--<td>${fieldValue(bean: featureInstance, field: "dbxref")}</td>--}%
					
						<td>${fieldValue(bean: featureInstance, field: "sequenceLength")}</td>
					
						%{--<td>${fieldValue(bean: featureInstance, field: "md5checksum")}</td>--}%
					
						%{--<td><g:formatBoolean boolean="${featureInstance.isAnalysis}" /></td>--}%
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${featureInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
