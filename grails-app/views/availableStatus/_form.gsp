<%@ page import="org.bbop.apollo.AvailableStatus" %>



<div class="fieldcontain ${hasErrors(bean: availableStatusInstance, field: 'value', 'error')} required">
	<label for="value">
		<g:message code="availableStatus.value.label" default="Value" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="value" required="" value="${availableStatusInstance?.value}"/>

</div>

