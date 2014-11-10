
<%@ page import="org.bbop.apollo.Gene" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'gene.label', default: 'Gene')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-gene" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="list-gene" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="name" title="${message(code: 'gene.name.label', default: 'Name')}" />
					
						%{--<g:sortableColumn property="uniqueName" title="${message(code: 'gene.uniqueName.label', default: 'Unique Name')}" />--}%
						<th>Count</th>
					
						<th><g:message code="gene.dbxref.label" default="Dbxref" /></th>
					
						<g:sortableColumn property="residues" title="${message(code: 'gene.residues.label', default: 'Residues')}" />
					
						<g:sortableColumn property="sequenceLength" title="${message(code: 'gene.sequenceLength.label', default: 'Sequence Length')}" />
					
						<g:sortableColumn property="md5checksum" title="${message(code: 'gene.md5checksum.label', default: 'Md5checksum')}" />
					
					</tr>
				</thead>
				<tbody>
				<g:each in="${geneInstanceList}" status="i" var="geneInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td><g:link action="show" id="${geneInstance.id}">${fieldValue(bean: geneInstance, field: "name")}</g:link></td>

						<td>
							${geneInstance?.childFeatureRelationships?.size()}
							${geneInstance?.parentFeatureRelationships?.size()}

						</td>
						%{--<td>${fieldValue(bean: geneInstance, field: "uniqueName")}</td>--}%
					
						<td>${fieldValue(bean: geneInstance, field: "dbxref")}</td>
					
						<td>${fieldValue(bean: geneInstance, field: "residues")}</td>
					
						<td>${fieldValue(bean: geneInstance, field: "sequenceLength")}</td>
					
						<td>${fieldValue(bean: geneInstance, field: "md5checksum")}</td>
					
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${geneInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
