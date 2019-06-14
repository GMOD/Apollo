<%@ page import="org.bbop.apollo.SuggestedName" %>



<div class="fieldcontain ${hasErrors(bean: suggestedNameInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="suggestedName.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${suggestedNameInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: suggestedNameInstance, field: 'metadata', 'error')} ">
	<label for="metadata">
		<g:message code="suggestedName.metadata.label" default="Metadata" />
		
	</label>
	<g:textField name="metadata" value="${suggestedNameInstance?.metadata}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: suggestedNameInstance, field: 'featureTypes', 'error')} ">
	<label for="featureTypes">
		<g:message code="suggestedName.featureTypes.label" default="Feature Types" />
		
	</label>
	<g:select name="featureTypes" from="${org.bbop.apollo.FeatureType.list()}"
              multiple="multiple"
              optionKey="id" size="10"
              optionValue="display"
              value="${suggestedNameInstance?.featureTypes*.id}" class="many-to-many"/>

</div>

<div class="fieldcontain ${hasErrors(bean: suggestedNameInstance, field: 'organisms', 'error')} ">
	<label for="organisms">
		<g:message code="suggestedName.organisms.label" default="Organisms" />

	</label>
	<g:select name="organisms" from="${org.bbop.apollo.Organism.list()}"
			  multiple="multiple"
			  optionKey="id" size="10"
			  optionValue="commonName"
			  value="${organismFilters?.organism?.id}" class="many-to-many"/>

</div>
