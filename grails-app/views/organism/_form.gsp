<%@ page import="org.bbop.apollo.Organism" %>



<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'directory', 'error')} ">
	<label for="directory">
		<g:message code="organism.directory.label" default="Directory" />
		
	</label>
	<g:textField name="directory" value="${organismInstance?.directory}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'abbreviation', 'error')} required">
	<label for="abbreviation">
		<g:message code="organism.abbreviation.label" default="Abbreviation" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="abbreviation" required="" value="${organismInstance?.abbreviation}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'comment', 'error')} required">
	<label for="comment">
		<g:message code="organism.comment.label" default="Comment" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="comment" required="" value="${organismInstance?.comment}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'commonName', 'error')} required">
	<label for="commonName">
		<g:message code="organism.commonName.label" default="Common Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="commonName" required="" value="${organismInstance?.commonName}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'genus', 'error')} required">
	<label for="genus">
		<g:message code="organism.genus.label" default="Genus" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="genus" required="" value="${organismInstance?.genus}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'organismDBXrefs', 'error')} ">
	<label for="organismDBXrefs">
		<g:message code="organism.organismDBXrefs.label" default="Organism DBX refs" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${organismInstance?.organismDBXrefs?}" var="o">
    <li><g:link controller="organismDBXref" action="show" id="${o.id}">${o?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="organismDBXref" action="create" params="['organism.id': organismInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'organismDBXref.label', default: 'OrganismDBXref')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'organismProperties', 'error')} ">
	<label for="organismProperties">
		<g:message code="organism.organismProperties.label" default="Organism Properties" />
		
	</label>
	<g:select name="organismProperties" from="${org.bbop.apollo.OrganismProperty.list()}" multiple="multiple" optionKey="id" size="5" value="${organismInstance?.organismProperties*.id}" class="many-to-many"/>

</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'sequences', 'error')} ">
	<label for="sequences">
		<g:message code="organism.sequences.label" default="Sequences" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${organismInstance?.sequences?}" var="s">
    <li><g:link controller="sequence" action="show" id="${s.id}">${s?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="sequence" action="create" params="['organism.id': organismInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'sequence.label', default: 'Sequence')])}</g:link>
</li>
</ul>


</div>

<div class="fieldcontain ${hasErrors(bean: organismInstance, field: 'species', 'error')} required">
	<label for="species">
		<g:message code="organism.species.label" default="Species" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="species" required="" value="${organismInstance?.species}"/>

</div>

