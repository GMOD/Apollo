<%@ page import="org.bbop.apollo.FeatureEvent" %>



<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'editor', 'error')} ">
	<label for="editor">
		<g:message code="featureEvent.editor.label" default="Editor" />
		
	</label>
	<g:select id="editor" name="editor.id" from="${org.bbop.apollo.User.list()}" optionKey="id" value="${featureEventInstance?.editor?.id}" class="many-to-one" noSelection="['null': '']"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'originalJsonCommand', 'error')} ">
	<label for="originalJsonCommand">
		<g:message code="featureEvent.originalJsonCommand.label" default="Original Json Command" />
		
	</label>
	<g:textField name="originalJsonCommand" value="${featureEventInstance?.originalJsonCommand}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'newFeaturesJsonArray', 'error')} ">
	<label for="newFeaturesJsonArray">
		<g:message code="featureEvent.newFeaturesJsonArray.label" default="New Features Json Array" />
		
	</label>
	<g:textField name="newFeaturesJsonArray" value="${featureEventInstance?.newFeaturesJsonArray}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'oldFeaturesJsonArray', 'error')} ">
	<label for="oldFeaturesJsonArray">
		<g:message code="featureEvent.oldFeaturesJsonArray.label" default="Old Features Json Array" />
		
	</label>
	<g:textField name="oldFeaturesJsonArray" value="${featureEventInstance?.oldFeaturesJsonArray}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'name', 'error')} required">
	<label for="name">
		<g:message code="featureEvent.name.label" default="Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="name" required="" value="${featureEventInstance?.name}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'uniqueName', 'error')} required">
	<label for="uniqueName">
		<g:message code="featureEvent.uniqueName.label" default="Unique Name" />
		<span class="required-indicator">*</span>
	</label>
	<g:textField name="uniqueName" required="" value="${featureEventInstance?.uniqueName}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'parentId', 'error')} ">
	<label for="parentId">
		<g:message code="featureEvent.parentId.label" default="Parent Id" />
		
	</label>
	<g:field name="parentId" type="number" value="${featureEventInstance.parentId}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'parentMergeId', 'error')} ">
	<label for="parentMergeId">
		<g:message code="featureEvent.parentMergeId.label" default="Parent Merge Id" />
		
	</label>
	<g:field name="parentMergeId" type="number" value="${featureEventInstance.parentMergeId}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'childId', 'error')} ">
	<label for="childId">
		<g:message code="featureEvent.childId.label" default="Child Id" />
		
	</label>
	<g:field name="childId" type="number" value="${featureEventInstance.childId}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'childSplitId', 'error')} ">
	<label for="childSplitId">
		<g:message code="featureEvent.childSplitId.label" default="Child Split Id" />
		
	</label>
	<g:field name="childSplitId" type="number" value="${featureEventInstance.childSplitId}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'current', 'error')} ">
	<label for="current">
		<g:message code="featureEvent.current.label" default="Current" />
		
	</label>
	<g:checkBox name="current" value="${featureEventInstance?.current}" />

</div>

<div class="fieldcontain ${hasErrors(bean: featureEventInstance, field: 'operation', 'error')} required">
	<label for="operation">
		<g:message code="featureEvent.operation.label" default="Operation" />
		<span class="required-indicator">*</span>
	</label>
	<g:select name="operation" from="${org.bbop.apollo.history.FeatureOperation?.values()}" keys="${org.bbop.apollo.history.FeatureOperation.values()*.name()}" required="" value="${featureEventInstance?.operation?.name()}" />

</div>

