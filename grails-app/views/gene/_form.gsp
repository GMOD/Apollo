<%@ page import="org.bbop.apollo.Gene" %>



<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="gene.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${geneInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'uniqueName', 'error')} ">
	<label for="uniqueName">
		<g:message code="gene.uniqueName.label" default="Unique Name" />
		
	</label>
	<g:textField name="uniqueName" value="${geneInstance?.uniqueName}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'dbxref', 'error')} ">
	<label for="dbxref">
		<g:message code="gene.dbxref.label" default="Dbxref" />
		
	</label>
	<g:select id="dbxref" name="dbxref.id" from="${org.bbop.apollo.DBXref.list()}" optionKey="id" value="${geneInstance?.dbxref?.id}" class="many-to-one" noSelection="['null': '']"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'residues', 'error')} ">
	<label for="residues">
		<g:message code="gene.residues.label" default="Residues" />
		
	</label>
	<g:textField name="residues" value="${geneInstance?.residues}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'sequenceLength', 'error')} ">
	<label for="sequenceLength">
		<g:message code="gene.sequenceLength.label" default="Sequence Length" />
		
	</label>
	<g:field name="sequenceLength" type="number" value="${geneInstance.sequenceLength}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'md5checksum', 'error')} ">
	<label for="md5checksum">
		<g:message code="gene.md5checksum.label" default="Md5checksum" />
		
	</label>
	<g:textField name="md5checksum" value="${geneInstance?.md5checksum}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'isAnalysis', 'error')} ">
	<label for="isAnalysis">
		<g:message code="gene.isAnalysis.label" default="Is Analysis" />
		
	</label>
	<g:checkBox name="isAnalysis" value="${geneInstance?.isAnalysis}" />

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'isObsolete', 'error')} ">
	<label for="isObsolete">
		<g:message code="gene.isObsolete.label" default="Is Obsolete" />
		
	</label>
	<g:checkBox name="isObsolete" value="${geneInstance?.isObsolete}" />

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'childFeatureRelationships', 'error')} ">
	<label for="childFeatureRelationships">
		<g:message code="gene.childFeatureRelationships.label" default="Child Feature Relationships" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.childFeatureRelationships?}" var="c">
    <li><g:link controller="featureRelationship" action="show" id="${c.id}">${c?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureRelationship" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureRelationship.label', default: 'FeatureRelationship')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featureCVTerms', 'error')} ">
	<label for="featureCVTerms">
		<g:message code="gene.featureCVTerms.label" default="Feature CVT erms" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featureCVTerms?}" var="f">
    <li><g:link controller="featureCVTerm" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureCVTerm" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureCVTerm.label', default: 'FeatureCVTerm')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featureDBXrefs', 'error')} ">
	<label for="featureDBXrefs">
		<g:message code="gene.featureDBXrefs.label" default="Feature DBX refs" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featureDBXrefs?}" var="f">
    <li><g:link controller="featureDBXref" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureDBXref" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureDBXref.label', default: 'FeatureDBXref')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featureGenotypes', 'error')} ">
	<label for="featureGenotypes">
		<g:message code="gene.featureGenotypes.label" default="Feature Genotypes" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featureGenotypes?}" var="f">
    <li><g:link controller="featureGenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureGenotype" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureGenotype.label', default: 'FeatureGenotype')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featureLocations', 'error')} ">
	<label for="featureLocations">
		<g:message code="gene.featureLocations.label" default="Feature Locations" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featureLocations?}" var="f">
    <li><g:link controller="featureLocation" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureLocation" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureLocation.label', default: 'FeatureLocation')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featurePhenotypes', 'error')} ">
	<label for="featurePhenotypes">
		<g:message code="gene.featurePhenotypes.label" default="Feature Phenotypes" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featurePhenotypes?}" var="f">
    <li><g:link controller="featurePhenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featurePhenotype" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featurePhenotype.label', default: 'FeaturePhenotype')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featureProperties', 'error')} ">
	<label for="featureProperties">
		<g:message code="gene.featureProperties.label" default="Feature Properties" />
		
	</label>
	

</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featurePublications', 'error')} ">
	<label for="featurePublications">
		<g:message code="gene.featurePublications.label" default="Feature Publications" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featurePublications?}" var="f">
    <li><g:link controller="featurePublication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featurePublication" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featurePublication.label', default: 'FeaturePublication')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'featureSynonyms', 'error')} ">
	<label for="featureSynonyms">
		<g:message code="gene.featureSynonyms.label" default="Feature Synonyms" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.featureSynonyms?}" var="f">
    <li><g:link controller="featureSynonym" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureSynonym" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureSynonym.label', default: 'FeatureSynonym')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: geneInstance, field: 'parentFeatureRelationships', 'error')} ">
	<label for="parentFeatureRelationships">
		<g:message code="gene.parentFeatureRelationships.label" default="Parent Feature Relationships" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${geneInstance?.parentFeatureRelationships?}" var="p">
    <li><g:link controller="featureRelationship" action="show" id="${p.id}">${p?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureRelationship" action="create" params="['gene.id': geneInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureRelationship.label', default: 'FeatureRelationship')])}</g:link>
</li>
</ul>


</div>

