
<%@ page import="org.bbop.apollo.Feature" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'feature.label', default: 'Feature')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-feature" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-feature" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list feature">
			
				<g:if test="${featureInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="feature.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${featureInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.uniqueName}">
				<li class="fieldcontain">
					<span id="uniqueName-label" class="property-label"><g:message code="feature.uniqueName.label" default="Unique Name" /></span>
					
						<span class="property-value" aria-labelledby="uniqueName-label"><g:fieldValue bean="${featureInstance}" field="uniqueName"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.dbxref}">
				<li class="fieldcontain">
					<span id="dbxref-label" class="property-label"><g:message code="feature.dbxref.label" default="Dbxref" /></span>
					
						<span class="property-value" aria-labelledby="dbxref-label"><g:link controller="DBXref" action="show" id="${featureInstance?.dbxref?.id}">${featureInstance?.dbxref?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.sequenceLength}">
				<li class="fieldcontain">
					<span id="sequenceLength-label" class="property-label"><g:message code="feature.sequenceLength.label" default="Sequence Length" /></span>
					
						<span class="property-value" aria-labelledby="sequenceLength-label"><g:fieldValue bean="${featureInstance}" field="sequenceLength"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.md5checksum}">
				<li class="fieldcontain">
					<span id="md5checksum-label" class="property-label"><g:message code="feature.md5checksum.label" default="Md5checksum" /></span>
					
						<span class="property-value" aria-labelledby="md5checksum-label"><g:fieldValue bean="${featureInstance}" field="md5checksum"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.isAnalysis}">
				<li class="fieldcontain">
					<span id="isAnalysis-label" class="property-label"><g:message code="feature.isAnalysis.label" default="Is Analysis" /></span>
					
						<span class="property-value" aria-labelledby="isAnalysis-label"><g:formatBoolean boolean="${featureInstance?.isAnalysis}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.isObsolete}">
				<li class="fieldcontain">
					<span id="isObsolete-label" class="property-label"><g:message code="feature.isObsolete.label" default="Is Obsolete" /></span>
					
						<span class="property-value" aria-labelledby="isObsolete-label"><g:formatBoolean boolean="${featureInstance?.isObsolete}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.dateCreated}">
				<li class="fieldcontain">
					<span id="dateCreated-label" class="property-label"><g:message code="feature.dateCreated.label" default="Date Created" /></span>
					
						<span class="property-value" aria-labelledby="dateCreated-label"><g:formatDate date="${featureInstance?.dateCreated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.lastUpdated}">
				<li class="fieldcontain">
					<span id="lastUpdated-label" class="property-label"><g:message code="feature.lastUpdated.label" default="Last Updated" /></span>
					
						<span class="property-value" aria-labelledby="lastUpdated-label"><g:formatDate date="${featureInstance?.lastUpdated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.symbol}">
				<li class="fieldcontain">
					<span id="symbol-label" class="property-label"><g:message code="feature.symbol.label" default="Symbol" /></span>
					
						<span class="property-value" aria-labelledby="symbol-label"><g:fieldValue bean="${featureInstance}" field="symbol"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.description}">
				<li class="fieldcontain">
					<span id="description-label" class="property-label"><g:message code="feature.description.label" default="Description" /></span>
					
						<span class="property-value" aria-labelledby="description-label"><g:fieldValue bean="${featureInstance}" field="description"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.status}">
				<li class="fieldcontain">
					<span id="status-label" class="property-label"><g:message code="feature.status.label" default="Status" /></span>
					
						<span class="property-value" aria-labelledby="status-label"><g:link controller="status" action="show" id="${featureInstance?.status?.id}">${featureInstance?.status?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.childFeatureRelationships}">
				<li class="fieldcontain">
					<span id="childFeatureRelationships-label" class="property-label"><g:message code="feature.childFeatureRelationships.label" default="Child Feature Relationships" /></span>
					
						<g:each in="${featureInstance.childFeatureRelationships}" var="c">
						<span class="property-value" aria-labelledby="childFeatureRelationships-label"><g:link controller="featureRelationship" action="show" id="${c.id}">${c?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featureCVTerms}">
				<li class="fieldcontain">
					<span id="featureCVTerms-label" class="property-label"><g:message code="feature.featureCVTerms.label" default="Feature CVT erms" /></span>
					
						<g:each in="${featureInstance.featureCVTerms}" var="f">
						<span class="property-value" aria-labelledby="featureCVTerms-label"><g:link controller="featureCVTerm" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featureDBXrefs}">
				<li class="fieldcontain">
					<span id="featureDBXrefs-label" class="property-label"><g:message code="feature.featureDBXrefs.label" default="Feature DBX refs" /></span>
					
						<g:each in="${featureInstance.featureDBXrefs}" var="f">
						<span class="property-value" aria-labelledby="featureDBXrefs-label"><g:link controller="DBXref" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featureGenotypes}">
				<li class="fieldcontain">
					<span id="featureGenotypes-label" class="property-label"><g:message code="feature.featureGenotypes.label" default="Feature Genotypes" /></span>
					
						<g:each in="${featureInstance.featureGenotypes}" var="f">
						<span class="property-value" aria-labelledby="featureGenotypes-label"><g:link controller="featureGenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featureLocations}">
				<li class="fieldcontain">
					<span id="featureLocations-label" class="property-label"><g:message code="feature.featureLocations.label" default="Feature Locations" /></span>
					
						<g:each in="${featureInstance.featureLocations}" var="f">
						<span class="property-value" aria-labelledby="featureLocations-label"><g:link controller="featureLocation" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featurePhenotypes}">
				<li class="fieldcontain">
					<span id="featurePhenotypes-label" class="property-label"><g:message code="feature.featurePhenotypes.label" default="Feature Phenotypes" /></span>
					
						<g:each in="${featureInstance.featurePhenotypes}" var="f">
						<span class="property-value" aria-labelledby="featurePhenotypes-label"><g:link controller="phenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featureProperties}">
				<li class="fieldcontain">
					<span id="featureProperties-label" class="property-label"><g:message code="feature.featureProperties.label" default="Feature Properties" /></span>
					
						<g:each in="${featureInstance.featureProperties}" var="f">
						<span class="property-value" aria-labelledby="featureProperties-label"><g:link controller="featureProperty" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featurePublications}">
				<li class="fieldcontain">
					<span id="featurePublications-label" class="property-label"><g:message code="feature.featurePublications.label" default="Feature Publications" /></span>
					
						<g:each in="${featureInstance.featurePublications}" var="f">
						<span class="property-value" aria-labelledby="featurePublications-label"><g:link controller="publication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.featureSynonyms}">
				<li class="fieldcontain">
					<span id="featureSynonyms-label" class="property-label"><g:message code="feature.featureSynonyms.label" default="Feature Synonyms" /></span>
					
						<g:each in="${featureInstance.featureSynonyms}" var="f">
						<span class="property-value" aria-labelledby="featureSynonyms-label"><g:link controller="featureSynonym" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.owners}">
				<li class="fieldcontain">
					<span id="owners-label" class="property-label"><g:message code="feature.owners.label" default="Owners" /></span>
					
						<g:each in="${featureInstance.owners}" var="o">
						<span class="property-value" aria-labelledby="owners-label"><g:link controller="user" action="show" id="${o.id}">${o?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.parentFeatureRelationships}">
				<li class="fieldcontain">
					<span id="parentFeatureRelationships-label" class="property-label"><g:message code="feature.parentFeatureRelationships.label" default="Parent Feature Relationships" /></span>
					
						<g:each in="${featureInstance.parentFeatureRelationships}" var="p">
						<span class="property-value" aria-labelledby="parentFeatureRelationships-label"><g:link controller="featureRelationship" action="show" id="${p.id}">${p?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${featureInstance?.synonyms}">
				<li class="fieldcontain">
					<span id="synonyms-label" class="property-label"><g:message code="feature.synonyms.label" default="Synonyms" /></span>
					
						<g:each in="${featureInstance.synonyms}" var="s">
						<span class="property-value" aria-labelledby="synonyms-label"><g:link controller="synonym" action="show" id="${s.id}">${s?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:featureInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${featureInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
