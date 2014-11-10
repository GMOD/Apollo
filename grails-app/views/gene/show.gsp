
<%@ page import="org.bbop.apollo.Gene" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'gene.label', default: 'Gene')}" />
		<title><g:message code="default.show.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#show-gene" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="index"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
				<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>
			</ul>
		</div>
		<div id="show-gene" class="content scaffold-show" role="main">
			<h1><g:message code="default.show.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<ol class="property-list gene">
			
				<g:if test="${geneInstance?.name}">
				<li class="fieldcontain">
					<span id="name-label" class="property-label"><g:message code="gene.name.label" default="Name" /></span>
					
						<span class="property-value" aria-labelledby="name-label"><g:fieldValue bean="${geneInstance}" field="name"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.uniqueName}">
				<li class="fieldcontain">
					<span id="uniqueName-label" class="property-label"><g:message code="gene.uniqueName.label" default="Unique Name" /></span>
					
						<span class="property-value" aria-labelledby="uniqueName-label"><g:fieldValue bean="${geneInstance}" field="uniqueName"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.dbxref}">
				<li class="fieldcontain">
					<span id="dbxref-label" class="property-label"><g:message code="gene.dbxref.label" default="Dbxref" /></span>
					
						<span class="property-value" aria-labelledby="dbxref-label"><g:link controller="DBXref" action="show" id="${geneInstance?.dbxref?.id}">${geneInstance?.dbxref?.encodeAsHTML()}</g:link></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.residues}">
				<li class="fieldcontain">
					<span id="residues-label" class="property-label"><g:message code="gene.residues.label" default="Residues" /></span>
					
						<span class="property-value" aria-labelledby="residues-label"><g:fieldValue bean="${geneInstance}" field="residues"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.sequenceLength}">
				<li class="fieldcontain">
					<span id="sequenceLength-label" class="property-label"><g:message code="gene.sequenceLength.label" default="Sequence Length" /></span>
					
						<span class="property-value" aria-labelledby="sequenceLength-label"><g:fieldValue bean="${geneInstance}" field="sequenceLength"/></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.md5checksum}">
				<li class="fieldcontain">
					<span id="md5checksum-label" class="property-label"><g:message code="gene.md5checksum.label" default="Md5checksum" /></span>
					
						<span class="property-value" aria-labelledby="md5checksum-label"><g:fieldValue bean="${geneInstance}" field="md5checksum"/></span>
					
				</li>
				</g:if>
			
				%{--<g:if test="${geneInstance?.isAnalysis}">--}%
				<li class="fieldcontain">
					<span id="isAnalysis-label" class="property-label"><g:message code="gene.isAnalysis.label" default="Is Analysis" /></span>
					
						<span class="property-value" aria-labelledby="isAnalysis-label"><g:formatBoolean boolean="${geneInstance?.isAnalysis}" /></span>
					
				</li>
				%{--</g:if>--}%
			
				%{--<g:if test="${geneInstance?.isObsolete}">--}%
				<li class="fieldcontain">
					<span id="isObsolete-label" class="property-label"><g:message code="gene.isObsolete.label" default="Is Obsolete" /></span>
					
						<span class="property-value" aria-labelledby="isObsolete-label"><g:formatBoolean boolean="${geneInstance?.isObsolete}" /></span>
					
				</li>
				%{--</g:if>--}%
			
				<g:if test="${geneInstance?.dateCreated}">
				<li class="fieldcontain">
					<span id="dateCreated-label" class="property-label"><g:message code="gene.dateCreated.label" default="Date Created" /></span>
					
						<span class="property-value" aria-labelledby="dateCreated-label"><g:formatDate date="${geneInstance?.dateCreated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.lastUpdated}">
				<li class="fieldcontain">
					<span id="lastUpdated-label" class="property-label"><g:message code="gene.lastUpdated.label" default="Last Updated" /></span>
					
						<span class="property-value" aria-labelledby="lastUpdated-label"><g:formatDate date="${geneInstance?.lastUpdated}" /></span>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.childFeatureRelationships}">
				<li class="fieldcontain">
					<span id="childFeatureRelationships-label" class="property-label"><g:message code="gene.childFeatureRelationships.label" default="Child Feature Relationships" /></span>
					
						<g:each in="${geneInstance.childFeatureRelationships}" var="c">
						<span class="property-value" aria-labelledby="childFeatureRelationships-label"><g:link controller="featureRelationship" action="show" id="${c.id}">${c?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.featureCVTerms}">
				<li class="fieldcontain">
					<span id="featureCVTerms-label" class="property-label"><g:message code="gene.featureCVTerms.label" default="Feature CVT erms" /></span>
					
						<g:each in="${geneInstance.featureCVTerms}" var="f">
						<span class="property-value" aria-labelledby="featureCVTerms-label"><g:link controller="featureCVTerm" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.featureDBXrefs}">
				<li class="fieldcontain">
					<span id="featureDBXrefs-label" class="property-label"><g:message code="gene.featureDBXrefs.label" default="Feature DBX refs" /></span>
					
						<g:each in="${geneInstance.featureDBXrefs}" var="f">
						<span class="property-value" aria-labelledby="featureDBXrefs-label"><g:link controller="featureDBXref" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.featureGenotypes}">
				<li class="fieldcontain">
					<span id="featureGenotypes-label" class="property-label"><g:message code="gene.featureGenotypes.label" default="Feature Genotypes" /></span>
					
						<g:each in="${geneInstance.featureGenotypes}" var="f">
						<span class="property-value" aria-labelledby="featureGenotypes-label"><g:link controller="featureGenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				%{--<g:if test="${geneInstance?.featureLocations}">--}%
				<li class="fieldcontain">
					<span id="featureLocations-label" class="property-label"><g:message code="gene.featureLocations.label" default="Feature Locations" /></span>
					
						<g:each in="${geneInstance.featureLocations}" var="featureLocation">
						<span class="property-value" aria-labelledby="featureLocations-label">
							<g:link controller="featureLocation" action="show" id="${featureLocation.id}">
								${featureLocation.fmin} - ${featureLocation.fmax}
						</g:link>
							Source Feature
							${sourceFeature ?: "None"}
						</span>
						</g:each>
					
				</li>
				%{--</g:if>--}%
			
				<g:if test="${geneInstance?.featurePhenotypes}">
				<li class="fieldcontain">
					<span id="featurePhenotypes-label" class="property-label"><g:message code="gene.featurePhenotypes.label" default="Feature Phenotypes" /></span>
					
						<g:each in="${geneInstance.featurePhenotypes}" var="f">
						<span class="property-value" aria-labelledby="featurePhenotypes-label"><g:link controller="featurePhenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.featureProperties}">
				<li class="fieldcontain">
					<span id="featureProperties-label" class="property-label"><g:message code="gene.featureProperties.label" default="Feature Properties" /></span>
					
						<g:each in="${geneInstance.featureProperties}" var="f">
						<span class="property-value" aria-labelledby="featurePoperties-label">
							<g:link controller="featureProperty" action="show" id="${f.id}">
								${f?.value}
							</g:link>
						</span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.featurePublications}">
				<li class="fieldcontain">
					<span id="featurePublications-label" class="property-label"><g:message code="gene.featurePublications.label" default="Feature Publications" /></span>
					
						<g:each in="${geneInstance.featurePublications}" var="f">
						<span class="property-value" aria-labelledby="featurePublications-label"><g:link controller="featurePublication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				<g:if test="${geneInstance?.featureSynonyms}">
				<li class="fieldcontain">
					<span id="featureSynonyms-label" class="property-label"><g:message code="gene.featureSynonyms.label" default="Feature Synonyms" /></span>
					
						<g:each in="${geneInstance.featureSynonyms}" var="f">
						<span class="property-value" aria-labelledby="featureSynonyms-label"><g:link controller="featureSynonym" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></span>
						</g:each>
					
				</li>
				</g:if>
			
				%{--<g:if test="${geneInstance?.parentFeatureRelationships}">--}%
				<li class="fieldcontain">
					<span id="parentFeatureRelationships-label" class="property-label"><g:message code="gene.parentFeatureRelationships.label" default="Parent Feature Relationships" /></span>
					
						<g:each in="${geneInstance.parentFeatureRelationships}" var="p">
						<span class="property-value" aria-labelledby="parentFeatureRelationships-label">
							%{--<g:link controller="featureRelationship" action="show" id="${p.id}">--}%
								<g:link controller="feature" action="show" id="${p.childFeature.id}">
								${p.childFeature.ontologyId}
								${p.childFeature.cvTerm}
							%{--${p?.encodeAsHTML()}--}%
						</g:link>
						</span>
						</g:each>
					
				</li>
				<li class="fieldcontain">
					<span id="childFeatureRelationships-label" class="property-label"><g:message code="gene.childFeatureRelationships.label" default="Child Feature Relationships" /></span>

					<g:each in="${geneInstance.childFeatureRelationships}" var="p">
						<span class="property-value" aria-labelledby="childFeatureRelationships-label"><g:link controller="featureRelationship" action="show" id="${p.id}">${p?.encodeAsHTML()}</g:link></span>
					</g:each>

				</li>
				%{--</g:if>--}%
			
			</ol>
			<g:form url="[resource:geneInstance, action:'delete']" method="DELETE">
				<fieldset class="buttons">
					<g:link class="edit" action="edit" resource="${geneInstance}"><g:message code="default.button.edit.label" default="Edit" /></g:link>
					<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
				</fieldset>
			</g:form>
		</div>
	</body>
</html>
