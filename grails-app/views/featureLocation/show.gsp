
<%@ page import="org.bbop.apollo.FeatureLocation" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'featureLocation.label', default: 'FeatureLocation')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-featureLocation" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-featureLocation" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list featureLocation">
			
				<g:if test="${featureLocationInstance?.feature}">
				<li class="fieldcontain">
					<span id="feature-label" class="property-label"><g:message code="featureLocation.feature.label" default="Feature" /></span>
					
						<span class="property-value" aria-labelledby="feature-label"><g:link controller="feature" action="show" id="${featureLocationInstance?.feature?.id}">${featureLocationInstance?.feature?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.fmin}">
				<li class="fieldcontain">
					<span id="fmin-label" class="property-label"><g:message code="featureLocation.fmin.label" default="Fmin" /></span>
					
						<span class="property-value" aria-labelledby="fmin-label"><g:fieldValue bean="${featureLocationInstance}" field="fmin"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.fmax}">
				<li class="fieldcontain">
					<span id="fmax-label" class="property-label"><g:message code="featureLocation.fmax.label" default="Fmax" /></span>
					
						<span class="property-value" aria-labelledby="fmax-label"><g:fieldValue bean="${featureLocationInstance}" field="fmax"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.isFminPartial}">
				<li class="fieldcontain">
					<span id="isFminPartial-label" class="property-label"><g:message code="featureLocation.isFminPartial.label" default="Is Fmin Partial" /></span>
					
						<span class="property-value" aria-labelledby="isFminPartial-label"><g:formatBoolean boolean="${featureLocationInstance?.isFminPartial}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.sourceFeature}">
				<li class="fieldcontain">
					<span id="sourceFeature-label" class="property-label"><g:message code="featureLocation.sourceFeature.label" default="Source Feature" /></span>
					
						<span class="property-value" aria-labelledby="sourceFeature-label"><g:link controller="feature" action="show" id="${featureLocationInstance?.sourceFeature?.id}">${featureLocationInstance?.sourceFeature?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.isFmaxPartial}">
				<li class="fieldcontain">
					<span id="isFmaxPartial-label" class="property-label"><g:message code="featureLocation.isFmaxPartial.label" default="Is Fmax Partial" /></span>
					
						<span class="property-value" aria-labelledby="isFmaxPartial-label"><g:formatBoolean boolean="${featureLocationInstance?.isFmaxPartial}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.strand}">
				<li class="fieldcontain">
					<span id="strand-label" class="property-label"><g:message code="featureLocation.strand.label" default="Strand" /></span>
					
						<span class="property-value" aria-labelledby="strand-label"><g:fieldValue bean="${featureLocationInstance}" field="strand"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.phase}">
				<li class="fieldcontain">
					<span id="phase-label" class="property-label"><g:message code="featureLocation.phase.label" default="Phase" /></span>
					
						<span class="property-value" aria-labelledby="phase-label"><g:fieldValue bean="${featureLocationInstance}" field="phase"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.residueInfo}">
				<li class="fieldcontain">
					<span id="residueInfo-label" class="property-label"><g:message code="featureLocation.residueInfo.label" default="Residue Info" /></span>
					
						<span class="property-value" aria-labelledby="residueInfo-label"><g:fieldValue bean="${featureLocationInstance}" field="residueInfo"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.locgroup}">
				<li class="fieldcontain">
					<span id="locgroup-label" class="property-label"><g:message code="featureLocation.locgroup.label" default="Locgroup" /></span>
					
						<span class="property-value" aria-labelledby="locgroup-label"><g:fieldValue bean="${featureLocationInstance}" field="locgroup"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.rank}">
				<li class="fieldcontain">
					<span id="rank-label" class="property-label"><g:message code="featureLocation.rank.label" default="Rank" /></span>
					
						<span class="property-value" aria-labelledby="rank-label"><g:fieldValue bean="${featureLocationInstance}" field="rank"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.sequence}">
				<li class="fieldcontain">
					<span id="sequence-label" class="property-label"><g:message code="featureLocation.sequence.label" default="Sequence" /></span>
					
						<span class="property-value" aria-labelledby="sequence-label"><g:link controller="sequence" action="show" id="${featureLocationInstance?.sequence?.id}">${featureLocationInstance?.sequence?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureLocationInstance?.featureLocationPublications}">
				<li class="fieldcontain">
					<span id="featureLocationPublications-label" class="property-label"><g:message code="featureLocation.featureLocationPublications.label" default="Feature Location Publications" /></span>
					
						<g:each in="${featureLocationInstance.featureLocationPublications}" var="f">
						<span class="property-value" aria-labelledby="featureLocationPublications-label"><g:link controller="featureLocationPublication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:featureLocationInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${featureLocationInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
