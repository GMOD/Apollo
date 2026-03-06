<%@ page import="org.bbop.apollo.GeneProductName" %>



<div class="fieldcontain ${hasErrors(bean: geneProductNameInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="geneProductName.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${geneProductNameInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductNameInstance, field: 'metadata', 'error')} ">
	<label for="metadata">
		<g:message code="geneProductName.metadata.label" default="Metadata" />
		
	</label>
	<g:textField name="metadata" value="${geneProductNameInstance?.metadata}"/>

</div>

%{--<div class="fieldcontain ${hasErrors(bean: geneProductNameInstance, field: 'featureTypes', 'error')} ">--}%
%{--	<label for="featureTypes">--}%
%{--		<g:message code="geneProductName.featureTypes.label" default="Feature Types" />--}%

%{--	</label>--}%
%{--	<g:select name="featureTypes" from="${org.bbop.apollo.FeatureType.list()}"--}%
%{--			  multiple="multiple"--}%
%{--			  optionKey="id" size="10"--}%
%{--			  optionValue="display"--}%
%{--			  value="${geneProductNameInstance?.featureTypes*.id}" class="many-to-many"/>--}%

%{--</div>--}%

<div class="fieldcontain ${hasErrors(bean: geneProductNameInstance, field: 'organisms', 'error')} ">
	<label for="organisms">
		<g:message code="geneProductName.organisms.label" default="Organisms" />

	</label>
	<g:select name="organisms" from="${org.bbop.apollo.Organism.list()}"
			  multiple="multiple"
			  optionKey="id" size="10"
			  optionValue="commonName"
			  value="${organismFilters?.organism?.id}" class="many-to-many"/>

</div>
