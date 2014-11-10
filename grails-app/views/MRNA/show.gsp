
<%@ page import="org.bbop.apollo.MRNA" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'MRNA.label', default: 'MRNA')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-MRNA" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-MRNA" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list MRNA">
			
				<g:if test="${MRNAInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="MRNA.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${MRNAInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.uniqueName}">
				<li class="fieldcontain">
					<span id="uniqueName-label" class="property-label"><g:message code="MRNA.uniqueName.label" default="Unique Name" /></span>
					
						<span class="property-value" aria-labelledby="uniqueName-label"><g:fieldValue bean="${MRNAInstance}" field="uniqueName"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.dbxref}">
				<li class="fieldcontain">
					<span id="dbxref-label" class="property-label"><g:message code="MRNA.dbxref.label" default="Dbxref" /></span>
					
						<span class="property-value" aria-labelledby="dbxref-label"><g:link controller="DBXref" action="show" id="${MRNAInstance?.dbxref?.id}">${MRNAInstance?.dbxref?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.residues}">
				<li class="fieldcontain">
					<span id="residues-label" class="property-label"><g:message code="MRNA.residues.label" default="Residues" /></span>
					
						<span class="property-value" aria-labelledby="residues-label"><g:fieldValue bean="${MRNAInstance}" field="residues"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.sequenceLength}">
				<li class="fieldcontain">
					<span id="sequenceLength-label" class="property-label"><g:message code="MRNA.sequenceLength.label" default="Sequence Length" /></span>
					
						<span class="property-value" aria-labelledby="sequenceLength-label"><g:fieldValue bean="${MRNAInstance}" field="sequenceLength"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.md5checksum}">
				<li class="fieldcontain">
					<span id="md5checksum-label" class="property-label"><g:message code="MRNA.md5checksum.label" default="Md5checksum" /></span>
					
						<span class="property-value" aria-labelledby="md5checksum-label"><g:fieldValue bean="${MRNAInstance}" field="md5checksum"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.isAnalysis}">
				<li class="fieldcontain">
					<span id="isAnalysis-label" class="property-label"><g:message code="MRNA.isAnalysis.label" default="Is Analysis" /></span>
					
						<span class="property-value" aria-labelledby="isAnalysis-label"><g:formatBoolean boolean="${MRNAInstance?.isAnalysis}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.isObsolete}">
				<li class="fieldcontain">
					<span id="isObsolete-label" class="property-label"><g:message code="MRNA.isObsolete.label" default="Is Obsolete" /></span>
					
						<span class="property-value" aria-labelledby="isObsolete-label"><g:formatBoolean boolean="${MRNAInstance?.isObsolete}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.dateCreated}">
				<li class="fieldcontain">
					<span id="dateCreated-label" class="property-label"><g:message code="MRNA.dateCreated.label" default="Date Created" /></span>
					
						<span class="property-value" aria-labelledby="dateCreated-label"><g:formatDate date="${MRNAInstance?.dateCreated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.lastUpdated}">
				<li class="fieldcontain">
					<span id="lastUpdated-label" class="property-label"><g:message code="MRNA.lastUpdated.label" default="Last Updated" /></span>
					
						<span class="property-value" aria-labelledby="lastUpdated-label"><g:formatDate date="${MRNAInstance?.lastUpdated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featureCVTerms}">
				<li class="fieldcontain">
					<span id="featureCVTerms-label" class="property-label"><g:message code="MRNA.featureCVTerms.label" default="Feature CVT erms" /></span>
					
						<g:each in="${MRNAInstance.featureCVTerms}" var="f">
						<span class="property-value" aria-labelledby="featureCVTerms-label"><g:link controller="featureCVTerm" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featureDBXrefs}">
				<li class="fieldcontain">
					<span id="featureDBXrefs-label" class="property-label"><g:message code="MRNA.featureDBXrefs.label" default="Feature DBX refs" /></span>
					
						<g:each in="${MRNAInstance.featureDBXrefs}" var="f">
						<span class="property-value" aria-labelledby="featureDBXrefs-label"><g:link controller="featureDBXref" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featureGenotypes}">
				<li class="fieldcontain">
					<span id="featureGenotypes-label" class="property-label"><g:message code="MRNA.featureGenotypes.label" default="Feature Genotypes" /></span>
					
						<g:each in="${MRNAInstance.featureGenotypes}" var="f">
						<span class="property-value" aria-labelledby="featureGenotypes-label"><g:link controller="featureGenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featureLocations}">
				<li class="fieldcontain">
					<span id="featureLocations-label" class="property-label"><g:message code="MRNA.featureLocations.label" default="Feature Locations" /></span>
					
						<g:each in="${MRNAInstance.featureLocations}" var="f">
						<span class="property-value" aria-labelledby="featureLocations-label">
							<g:link controller="featureLocation" action="show" id="${f.id}">${f?.fmin}-${f.fmax} : ${f.strand ? '+' : '-'}</g:link>
						</span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featurePhenotypes}">
				<li class="fieldcontain">
					<span id="featurePhenotypes-label" class="property-label"><g:message code="MRNA.featurePhenotypes.label" default="Feature Phenotypes" /></span>
					
						<g:each in="${MRNAInstance.featurePhenotypes}" var="f">
						<span class="property-value" aria-labelledby="featurePhenotypes-label"><g:link controller="featurePhenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featureProperties}">
				<li class="fieldcontain">
					<span id="featureProperties-label" class="property-label"><g:message code="MRNA.featureProperties.label" default="Feature Properties" /></span>
					
						<g:each in="${MRNAInstance.featureProperties}" var="f">
						<span class="property-value" aria-labelledby="featureProperties-label"><g:link controller="featureProperty" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featurePublications}">
				<li class="fieldcontain">
					<span id="featurePublications-label" class="property-label"><g:message code="MRNA.featurePublications.label" default="Feature Publications" /></span>
					
						<g:each in="${MRNAInstance.featurePublications}" var="f">
						<span class="property-value" aria-labelledby="featurePublications-label"><g:link controller="featurePublication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${MRNAInstance?.featureSynonyms}">
				<li class="fieldcontain">
					<span id="featureSynonyms-label" class="property-label"><g:message code="MRNA.featureSynonyms.label" default="Feature Synonyms" /></span>
					
						<g:each in="${MRNAInstance.featureSynonyms}" var="f">
						<span class="property-value" aria-labelledby="featureSynonyms-label"><g:link controller="featureSynonym" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>

				<g:if test="${MRNAInstance?.childFeatureRelationships}">
					<li class="fieldcontain">
						<span id="childFeatureRelationships-label" class="property-label"><g:message code="MRNA.childFeatureRelationships.label" default="Parents" /></span>

						<g:each in="${MRNAInstance.childFeatureRelationships}" var="c">
							<span class="property-value" aria-labelledby="childFeatureRelationships-label">
								<g:link controller="feature" action="show" id="${c.parentFeature.id}">
									${c.parentFeature.ontologyId}
									${c.parentFeature.cvTerm}
								${c?.parentFeature.name}
								</g:link></span>
						</g:each>

					</li>
				</g:if>


				<g:if test="${MRNAInstance?.parentFeatureRelationships}">
				<li class="fieldcontain">
					<span id="parentFeatureRelationships-label" class="property-label"><g:message code="MRNA.parentFeatureRelationships.label" default="Children" /></span>
					
						<g:each in="${MRNAInstance.parentFeatureRelationships}" var="p">
						<span class="property-value" aria-labelledby="parentFeatureRelationships-label">
							<g:link controller="feature" action="show" id="${p.childFeature.id}">
							${p.childFeature.ontologyId}
							${p.childFeature.cvTerm}
							${p.childFeature.name}
							%{--${p?.encodeAsHTML()}--}%
						</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
			</ol>
			<g:form url="[resource:MRNAInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${MRNAInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
