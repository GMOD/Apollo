<%@ page import="org.bbop.apollo.MRNA" %>



<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="MRNA.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${MRNAInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'uniqueName', 'error')} ">
	<label for="uniqueName">
		<g:message code="MRNA.uniqueName.label" default="Unique Name" />
		
	</label>
	<g:textField name="uniqueName" value="${MRNAInstance?.uniqueName}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'dbxref', 'error')} ">
	<label for="dbxref">
		<g:message code="MRNA.dbxref.label" default="Dbxref" />
		
	</label>
	<g:select id="dbxref" name="dbxref.id" from="${org.bbop.apollo.DBXref.list()}" optionKey="id" value="${MRNAInstance?.dbxref?.id}" class="many-to-one" noSelection="['null': '']"/>

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'residues', 'error')} ">
	<label for="residues">
		<g:message code="MRNA.residues.label" default="Residues" />
		
	</label>
	<g:textField name="residues" value="${MRNAInstance?.residues}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'sequenceLength', 'error')} ">
	<label for="sequenceLength">
		<g:message code="MRNA.sequenceLength.label" default="Sequence Length" />
		
	</label>
	<g:field name="sequenceLength" type="number" value="${MRNAInstance.sequenceLength}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'md5checksum', 'error')} ">
	<label for="md5checksum">
		<g:message code="MRNA.md5checksum.label" default="Md5checksum" />
		
	</label>
	<g:textField name="md5checksum" value="${MRNAInstance?.md5checksum}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'isAnalysis', 'error')} ">
	<label for="isAnalysis">
		<g:message code="MRNA.isAnalysis.label" default="Is Analysis" />
		
	</label>
	<g:checkBox name="isAnalysis" value="${MRNAInstance?.isAnalysis}" />

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'isObsolete', 'error')} ">
	<label for="isObsolete">
		<g:message code="MRNA.isObsolete.label" default="Is Obsolete" />
		
	</label>
	<g:checkBox name="isObsolete" value="${MRNAInstance?.isObsolete}" />

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'childFeatureRelationships', 'error')} ">
	<label for="childFeatureRelationships">
		<g:message code="MRNA.childFeatureRelationships.label" default="Child Feature Relationships" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.childFeatureRelationships?}" var="c">
    <li><g:link controller="featureRelationship" action="show" id="${c.id}">${c?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureRelationship" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureRelationship.label', default: 'FeatureRelationship')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featureCVTerms', 'error')} ">
	<label for="featureCVTerms">
		<g:message code="MRNA.featureCVTerms.label" default="Feature CVT erms" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featureCVTerms?}" var="f">
    <li><g:link controller="featureCVTerm" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureCVTerm" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureCVTerm.label', default: 'FeatureCVTerm')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featureDBXrefs', 'error')} ">
	<label for="featureDBXrefs">
		<g:message code="MRNA.featureDBXrefs.label" default="Feature DBX refs" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featureDBXrefs?}" var="f">
    <li><g:link controller="featureDBXref" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureDBXref" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureDBXref.label', default: 'FeatureDBXref')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featureGenotypes', 'error')} ">
	<label for="featureGenotypes">
		<g:message code="MRNA.featureGenotypes.label" default="Feature Genotypes" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featureGenotypes?}" var="f">
    <li><g:link controller="featureGenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureGenotype" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureGenotype.label', default: 'FeatureGenotype')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featureLocations', 'error')} ">
	<label for="featureLocations">
		<g:message code="MRNA.featureLocations.label" default="Feature Locations" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featureLocations?}" var="f">
    <li><g:link controller="featureLocation" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureLocation" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureLocation.label', default: 'FeatureLocation')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featurePhenotypes', 'error')} ">
	<label for="featurePhenotypes">
		<g:message code="MRNA.featurePhenotypes.label" default="Feature Phenotypes" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featurePhenotypes?}" var="f">
    <li><g:link controller="featurePhenotype" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featurePhenotype" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featurePhenotype.label', default: 'FeaturePhenotype')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featureProperties', 'error')} ">
	<label for="featureProperties">
		<g:message code="MRNA.featureProperties.label" default="Feature Properties" />
		
	</label>
	

</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featurePublications', 'error')} ">
	<label for="featurePublications">
		<g:message code="MRNA.featurePublications.label" default="Feature Publications" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featurePublications?}" var="f">
    <li><g:link controller="featurePublication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featurePublication" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featurePublication.label', default: 'FeaturePublication')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'featureSynonyms', 'error')} ">
	<label for="featureSynonyms">
		<g:message code="MRNA.featureSynonyms.label" default="Feature Synonyms" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.featureSynonyms?}" var="f">
    <li><g:link controller="featureSynonym" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureSynonym" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureSynonym.label', default: 'FeatureSynonym')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: MRNAInstance, field: 'parentFeatureRelationships', 'error')} ">
	<label for="parentFeatureRelationships">
		<g:message code="MRNA.parentFeatureRelationships.label" default="Parent Feature Relationships" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${MRNAInstance?.parentFeatureRelationships?}" var="p">
    <li><g:link controller="featureRelationship" action="show" id="${p.id}">${p?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureRelationship" action="create" params="['MRNA.id': MRNAInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureRelationship.label', default: 'FeatureRelationship')])}</g:link>
</li>
</ul>


</div>

