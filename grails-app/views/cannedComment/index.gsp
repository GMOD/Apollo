
<%@ page import="org.bbop.apollo.CannedComment" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'cannedComment.label', default: 'CannedComment')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-cannedComment" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-cannedComment" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="comment" title="${message(code: 'cannedComment.comment.label', default: 'Comment')}" />

						%{--<g:sortableColumn property="metadata" title="${message(code: 'cannedComment.featureTypes.label', default: 'Feature Types')}" />--}%
						<th>Feature Types</th>
						<th>Organisms</th>
						<g:sortableColumn property="metadata" title="${message(code: 'cannedComment.metadata.label', default: 'Metadata')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${cannedCommentInstanceList}" status="i" var="cannedCommentInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${cannedCommentInstance.id}">${fieldValue(bean: cannedCommentInstance, field: "comment")}</g:link></td>

						<td>
							<g:each in="${cannedCommentInstance.featureTypes.sort() { a,b -> a.display <=> b.display }}" var="featureType">
								${featureType.type}:${featureType.name}
							</g:each>
						</td>
						<td>
							<g:each in="${organismFilters.get(cannedCommentInstance)}" var="filter">
								<g:link controller="organism" id="${filter.organism.id}">${filter.organism.commonName}</g:link>
							</g:each>
						</td>

						<td>${fieldValue(bean: cannedCommentInstance, field: "metadata")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${cannedCommentInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
