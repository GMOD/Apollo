<%@ page import="org.bbop.apollo.geneProduct.GeneProduct" %>



<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'feature', 'error')} required">
	<label for="feature">
		<g:message code="geneProduct.feature.label" default="Feature" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="feature" name="feature.id" from="${org.bbop.apollo.Feature.list()}" optionKey="id" required="" value="${geneProductInstance?.feature?.id}" class="many-to-one"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'productName', 'error')} required">
	<label for="productName">
		<g:message code="geneProduct.productName.label" default="Product Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="productName" required="" value="${geneProductInstance?.productName}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'reference', 'error')} required">
	<label for="reference">
		<g:message code="geneProduct.reference.label" default="Reference" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="reference" required="" value="${geneProductInstance?.reference}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'evidenceRef', 'error')} required">
	<label for="evidenceRef">
		<g:message code="geneProduct.evidenceRef.label" default="Evidence Ref" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="evidenceRef" required="" value="${geneProductInstance?.evidenceRef}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'evidenceRefLabel', 'error')} ">
	<label for="evidenceRefLabel">
		<g:message code="geneProduct.evidenceRefLabel.label" default="Evidence Ref Label" />
		
	</label>
	<g:textField name="evidenceRefLabel" value="${geneProductInstance?.evidenceRefLabel}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'withOrFromArray', 'error')} ">
	<label for="withOrFromArray">
		<g:message code="geneProduct.withOrFromArray.label" default="With Or From Array" />
		
	</label>
	<g:textField name="withOrFromArray" value="${geneProductInstance?.withOrFromArray}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'alternate', 'error')} ">
	<label for="alternate">
		<g:message code="geneProduct.alternate.label" default="Alternate" />
		
	</label>
	<g:checkBox name="alternate" value="${geneProductInstance?.alternate}" />

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'notesArray', 'error')} ">
	<label for="notesArray">
		<g:message code="geneProduct.notesArray.label" default="Notes Array" />
		
	</label>
	<g:textField name="notesArray" value="${geneProductInstance?.notesArray}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: geneProductInstance, field: 'owners', 'error')} ">
	<label for="owners">
		<g:message code="geneProduct.owners.label" default="Owners" />
		
	</label>
	<g:select name="owners" from="${org.bbop.apollo.User.list()}" multiple="multiple" optionKey="id" size="5" value="${geneProductInstance?.owners*.id}" class="many-to-many"/>

</div>

