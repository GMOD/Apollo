<%@ page import="org.bbop.apollo.Sequence" %>



<div class="fieldcontain ${hasErrors(bean: sequenceInstance, field: 'genome', 'error')} ">
	<label for="genome">
		<g:message code="sequence.genome.label" default="Genome" />
		
	</label>
	<g:select id="genome" name="genome.id" from="${org.bbop.apollo.Genome.list()}" optionValue="name" optionKey="id" value="${sequenceInstance?.genome?.id}" class="many-to-one" noSelection="['null': '']"/>

</div>

<div class="fieldcontain ${hasErrors(bean: sequenceInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="sequence.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${sequenceInstance?.name}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: sequenceInstance, field: 'sequenceCV', 'error')} required">
	<label for="type">
		<g:message code="sequence.type.label" default="CV" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="sequenceCV" required="" value="${sequenceInstance?.sequenceCV}"/>
</div>

<div class="fieldcontain ${hasErrors(bean: sequenceInstance, field: 'sequenceType', 'error')} required">
	<label for="sequenceType">
		<g:message code="sequence.sequenceType.label" default="Type" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="sequenceType" required="" value="${sequenceInstance?.sequenceType}"/>
</div>

%{--<div class="fieldcontain ${hasErrors(bean: sequenceInstance, field: 'users', 'error')} ">--}%
	%{--<label for="users">--}%
		%{--<g:message code="sequence.users.label" default="Users" />--}%
		%{----}%
	%{--</label>--}%
	%{----}%

%{--</div>--}%

