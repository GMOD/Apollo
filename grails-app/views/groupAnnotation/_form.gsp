<%@ page import="org.bbop.apollo.GroupAnnotation" %>



<div class="fieldcontain ${hasErrors(bean: groupInstance, field: 'name', 'error')} ">
	<label for="name">
		<g:message code="group.name.label" default="Name" />
		
	</label>
	<g:textField name="name" value="${groupInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: groupInstance, field: 'sequences', 'error')} ">
	<label for="sequences">
		<g:message code="group.sequences.label" default="Sequences" />
		
	</label>
	

</div>

