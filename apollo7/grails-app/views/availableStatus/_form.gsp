<%@ page import="org.bbop.apollo.AvailableStatus" %>



<div class="fieldcontain ${hasErrors(bean: availableStatusInstance, field: 'value', 'error')} required">
	<label for="value">
		<g:message code="availableStatus.value.label" default="Value" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="value" required="" value="${availableStatusInstance?.value}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: availableStatusInstance, field: 'featureTypes', 'error')} ">
	<label for="featureTypes">
		<g:message code="availableStatus.featureTypes.label" default="Feature Types" />

	</label>
	<g:select name="featureTypes" from="${org.bbop.apollo.FeatureType.list()}"
			  multiple="multiple"
			  optionKey="id" size="10"
			  optionValue="display"
			  value="${availableStatusInstance?.featureTypes*.id}" class="many-to-many"/>

</div>

<div class="fieldcontain ${hasErrors(bean: availableStatusInstance, field: 'organisms', 'error')} ">
	<label for="organisms">
		<g:message code="availableStatus.organisms.label" default="Organisms" />

	</label>
	<g:select name="organisms" from="${org.bbop.apollo.Organism.list()}"
			  multiple="multiple"
			  optionKey="id" size="10"
			  optionValue="commonName"
			  value="${organismFilters?.organism?.id}" class="many-to-many"/>

</div>
