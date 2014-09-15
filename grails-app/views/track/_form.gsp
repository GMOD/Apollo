<%@ page import="org.bbop.apollo.Track" %>



<div class="fieldcontain ${hasErrors(bean: trackInstance, field: 'genome', 'error')} ">
	<label for="genome">
		<g:message code="track.genome.label" default="Genome" />
		
	</label>
	<g:select id="genome" name="genome.id" from="${org.bbop.apollo.Genome.list()}" optionKey="id" value="${trackInstance?.genome?.id}" class="many-to-one" noSelection="['null': '']"/>

</div>

<div class="fieldcontain ${hasErrors(bean: trackInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="track.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${trackInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: trackInstance, field: 'users', 'error')} ">
	<label for="users">
		<g:message code="track.users.label" default="Users" />
		
	</label>
	

</div>

