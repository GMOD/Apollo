
<%@ page import="org.bbop.apollo.FeatureEvent" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'featureEvent.label', default: 'FeatureEvent')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-featureEvent" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-featureEvent" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list featureEvent">
			
				<g:if test="${featureEventInstance?.editor}">
				<li class="fieldcontain">
					<span id="editor-label" class="property-label"><g:message code="featureEvent.editor.label" default="Editor" /></span>
					
						<span class="property-value" aria-labelledby="editor-label"><g:link controller="user" action="show" id="${featureEventInstance?.editor?.id}">${featureEventInstance?.editor?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.originalJsonCommand}">
				<li class="fieldcontain">
					<span id="originalJsonCommand-label" class="property-label"><g:message code="featureEvent.originalJsonCommand.label" default="Original Json Command" /></span>
					
						<span class="property-value" aria-labelledby="originalJsonCommand-label"><g:fieldValue bean="${featureEventInstance}" field="originalJsonCommand"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.newFeaturesJsonArray}">
				<li class="fieldcontain">
					<span id="newFeaturesJsonArray-label" class="property-label"><g:message code="featureEvent.newFeaturesJsonArray.label" default="New Features Json Array" /></span>
					
						<span class="property-value" aria-labelledby="newFeaturesJsonArray-label"><g:fieldValue bean="${featureEventInstance}" field="newFeaturesJsonArray"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.oldFeaturesJsonArray}">
				<li class="fieldcontain">
					<span id="oldFeaturesJsonArray-label" class="property-label"><g:message code="featureEvent.oldFeaturesJsonArray.label" default="Old Features Json Array" /></span>
					
						<span class="property-value" aria-labelledby="oldFeaturesJsonArray-label"><g:fieldValue bean="${featureEventInstance}" field="oldFeaturesJsonArray"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="featureEvent.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${featureEventInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.uniqueName}">
				<li class="fieldcontain">
					<span id="uniqueName-label" class="property-label"><g:message code="featureEvent.uniqueName.label" default="Unique Name" /></span>
					
						<span class="property-value" aria-labelledby="uniqueName-label"><g:fieldValue bean="${featureEventInstance}" field="uniqueName"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.parentId}">
				<li class="fieldcontain">
					<span id="parentId-label" class="property-label"><g:message code="featureEvent.parentId.label" default="Parent Id" /></span>
					
						<span class="property-value" aria-labelledby="parentId-label"><g:fieldValue bean="${featureEventInstance}" field="parentId"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.parentMergeId}">
				<li class="fieldcontain">
					<span id="parentMergeId-label" class="property-label"><g:message code="featureEvent.parentMergeId.label" default="Parent Merge Id" /></span>
					
						<span class="property-value" aria-labelledby="parentMergeId-label"><g:fieldValue bean="${featureEventInstance}" field="parentMergeId"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.childId}">
				<li class="fieldcontain">
					<span id="childId-label" class="property-label"><g:message code="featureEvent.childId.label" default="Child Id" /></span>
					
						<span class="property-value" aria-labelledby="childId-label"><g:fieldValue bean="${featureEventInstance}" field="childId"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.childSplitId}">
				<li class="fieldcontain">
					<span id="childSplitId-label" class="property-label"><g:message code="featureEvent.childSplitId.label" default="Child Split Id" /></span>
					
						<span class="property-value" aria-labelledby="childSplitId-label"><g:fieldValue bean="${featureEventInstance}" field="childSplitId"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.current}">
				<li class="fieldcontain">
					<span id="current-label" class="property-label"><g:message code="featureEvent.current.label" default="Current" /></span>
					
						<span class="property-value" aria-labelledby="current-label"><g:formatBoolean boolean="${featureEventInstance?.current}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.dateCreated}">
				<li class="fieldcontain">
					<span id="dateCreated-label" class="property-label"><g:message code="featureEvent.dateCreated.label" default="Date Created" /></span>
					
						<span class="property-value" aria-labelledby="dateCreated-label"><g:formatDate date="${featureEventInstance?.dateCreated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.lastUpdated}">
				<li class="fieldcontain">
					<span id="lastUpdated-label" class="property-label"><g:message code="featureEvent.lastUpdated.label" default="Last Updated" /></span>
					
						<span class="property-value" aria-labelledby="lastUpdated-label"><g:formatDate date="${featureEventInstance?.lastUpdated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureEventInstance?.operation}">
				<li class="fieldcontain">
					<span id="operation-label" class="property-label"><g:message code="featureEvent.operation.label" default="Operation" /></span>
					
						<span class="property-value" aria-labelledby="operation-label"><g:fieldValue bean="${featureEventInstance}" field="operation"/></span>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:featureEventInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${featureEventInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
