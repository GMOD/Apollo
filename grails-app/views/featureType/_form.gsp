<%@ page import="org.bbop.apollo.FeatureType" %>



<div class="fieldcontain ${hasErrors(bean: featureTypeInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="featureType.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${featureTypeInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureTypeInstance, field: 'display', 'error')} required">
	<label for="display">
		<g:message code="featureType.display.label" default="Display" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="display" required="" value="${featureTypeInstance?.display}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureTypeInstance, field: 'ontologyId', 'error')} required">
	<label for="ontologyId">
		<g:message code="featureType.ontologyId.label" default="Ontology Id" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="ontologyId" required="" value="${featureTypeInstance?.ontologyId}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureTypeInstance, field: 'type', 'error')} required">
	<label for="type">
		<g:message code="featureType.type.label" default="Type" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="type" required="" value="${featureTypeInstance?.type}"/>

</div>

