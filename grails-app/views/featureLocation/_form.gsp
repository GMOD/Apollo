<%@ page import="org.bbop.apollo.FeatureLocation" %>



<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'feature', 'error')} required">
	<label for="feature">
		<g:message code="featureLocation.feature.label" default="Feature" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="feature" name="feature.id" from="${org.bbop.apollo.Feature.list()}" optionKey="id" required="" value="${featureLocationInstance?.feature?.id}" class="many-to-one"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'fmin', 'error')} required">
	<label for="fmin">
		<g:message code="featureLocation.fmin.label" default="Fmin" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="fmin" type="number" value="${featureLocationInstance.fmin}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'fmax', 'error')} required">
	<label for="fmax">
		<g:message code="featureLocation.fmax.label" default="Fmax" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="fmax" type="number" value="${featureLocationInstance.fmax}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'isFminPartial', 'error')} ">
	<label for="isFminPartial">
		<g:message code="featureLocation.isFminPartial.label" default="Is Fmin Partial" />
		
	</label>
	<g:checkBox name="isFminPartial" value="${featureLocationInstance?.isFminPartial}" />

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'sourceFeature', 'error')} ">
	<label for="sourceFeature">
		<g:message code="featureLocation.sourceFeature.label" default="Source Feature" />
		
	</label>
	<g:select id="sourceFeature" name="sourceFeature.id" from="${org.bbop.apollo.Feature.list()}" optionKey="id" value="${featureLocationInstance?.sourceFeature?.id}" class="many-to-one" noSelection="['null': '']"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'isFmaxPartial', 'error')} ">
	<label for="isFmaxPartial">
		<g:message code="featureLocation.isFmaxPartial.label" default="Is Fmax Partial" />
		
	</label>
	<g:checkBox name="isFmaxPartial" value="${featureLocationInstance?.isFmaxPartial}" />

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'strand', 'error')} ">
	<label for="strand">
		<g:message code="featureLocation.strand.label" default="Strand" />
		
	</label>
	<g:field name="strand" type="number" value="${featureLocationInstance.strand}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'phase', 'error')} ">
	<label for="phase">
		<g:message code="featureLocation.phase.label" default="Phase" />
		
	</label>
	<g:field name="phase" type="number" value="${featureLocationInstance.phase}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'residueInfo', 'error')} ">
	<label for="residueInfo">
		<g:message code="featureLocation.residueInfo.label" default="Residue Info" />
		
	</label>
	<g:textField name="residueInfo" value="${featureLocationInstance?.residueInfo}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'locgroup', 'error')} required">
	<label for="locgroup">
		<g:message code="featureLocation.locgroup.label" default="Locgroup" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="locgroup" type="number" value="${featureLocationInstance.locgroup}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'rank', 'error')} required">
	<label for="rank">
		<g:message code="featureLocation.rank.label" default="Rank" />
		<span class="required-indicator">*</span>
	</label>
	<g:field name="rank" type="number" value="${featureLocationInstance.rank}" required=""/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'sequence', 'error')} required">
	<label for="sequence">
		<g:message code="featureLocation.sequence.label" default="Sequence" />
		<span class="required-indicator">*</span>
	</label>
	<g:select id="sequence" name="sequence.id" from="${org.bbop.apollo.Sequence.list()}" optionKey="id" required="" value="${featureLocationInstance?.sequence?.id}" class="many-to-one"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureLocationInstance, field: 'featureLocationPublications', 'error')} ">
	<label for="featureLocationPublications">
		<g:message code="featureLocation.featureLocationPublications.label" default="Feature Location Publications" />
		
	</label>
	
<ul class="one-to-many">
<g:each in="${featureLocationInstance?.featureLocationPublications?}" var="f">
    <li><g:link controller="featureLocationPublication" action="show" id="${f.id}">${f?.encodeAsHTML()}</g:link></li>
</g:each>
<li class="add">
<g:link controller="featureLocationPublication" action="create" params="['featureLocation.id': featureLocationInstance?.id]">${message(code: 'default.add.label', args: [message(code: 'featureLocationPublication.label', default: 'FeatureLocationPublication')])}</g:link>
</li>
</ul>


</div>

