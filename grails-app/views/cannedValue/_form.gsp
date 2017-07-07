<%@ page import="org.bbop.apollo.CannedValue" %>



<div class="fieldcontain ${hasErrors(bean: cannedValueInstance, field: 'label', 'error')} required">
	<label for="label">
		<g:message code="cannedValue.label.label" default="Label" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="label" required="" value="${cannedValueInstance?.label}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: cannedValueInstance, field: 'metadata', 'error')} ">
	<label for="metadata">
		<g:message code="cannedValue.metadata.label" default="Metadata" />
		
	</label>
	<g:textField name="metadata" value="${cannedValueInstance?.metadata}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: cannedValueInstance, field: 'featureTypes', 'error')} ">
	<label for="featureTypes">
		<g:message code="cannedValue.featureTypes.label" default="Feature Types" />
		
	</label>
	<g:select name="featureTypes" from="${org.bbop.apollo.FeatureType.list()}" multiple="multiple" optionKey="id" size="10" value="${cannedValueInstance?.featureTypes*.id}" class="many-to-many" optionValue="display"/>

</div>

<div class="fieldcontain ${hasErrors(bean: cannedValueInstance, field: 'organisms', 'error')} ">
	<label for="organisms">
		<g:message code="cannedValue.organisms.label" default="Organisms" />

	</label>
	<g:select name="organisms" from="${org.bbop.apollo.Organism.list()}"
			  multiple="multiple"
			  optionKey="id" size="10"
			  optionValue="commonName"
			  value="${organismFilters?.organism?.id}" class="many-to-many"/>

</div>
