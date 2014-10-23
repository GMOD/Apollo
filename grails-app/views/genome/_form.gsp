<%@ page import="org.bbop.apollo.Genome" %>



<div class="fieldcontain ${hasErrors(bean: genomeInstance, field: 'directory', 'error')} ">
	<label for="directory">
		<g:message code="genome.directory.label" default="Directory" />
		
	</label>
	<g:textField name="directory" value="${genomeInstance?.directory}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: genomeInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="genome.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${genomeInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: genomeInstance, field: 'sequences', 'error')} ">
	<label for="sequences">
		<g:message code="genome.sequences.label" default="Sequences" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${genomeInstance?.sequences?}" var="t">
    <li><g:link controller="sequence" action="show" id="${t.id}">${t?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="sequence" action="create" params="['genome.id': genomeInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'sequence.label', default: 'Sequence')])}</g:link>
</li>
</ul>


</div>

