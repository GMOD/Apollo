<%@ page import="org.bbop.apollo.Group" %>



<div class="fieldcontain ${hasErrors(bean: groupInstance, field: 'name', 'error')} ">
	<label for="name">
		<g:message code="group.name.label" default="Name" />
		
	</label>
	<g:textField name="name" value="${groupInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: groupInstance, field: 'tracks', 'error')} ">
	<label for="tracks">
		<g:message code="group.tracks.label" default="Tracks" />
		
	</label>
	

</div>

