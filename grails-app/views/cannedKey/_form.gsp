<%@ page import="org.bbop.apollo.CannedKey" %>



<div class="fieldcontain ${hasErrors(bean: cannedKeyInstance, field: 'label', 'error')} required">
    <label for="label">
        <g:message code="cannedKey.label.label" default="Label"/>
        <span class="required-indicator">*</span>
    </label>
    <g:textField name="label" required="" value="${cannedKeyInstance?.label}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: cannedKeyInstance, field: 'metadata', 'error')} ">
    <label for="metadata">
        <g:message code="cannedKey.metadata.label" default="Metadata"/>

    </label>
    <g:textField name="metadata" value="${cannedKeyInstance?.metadata}"/>

</div>

<div class="fieldcontain ${hasErrors(bean: cannedKeyInstance, field: 'featureTypes', 'error')} ">
    <label for="featureTypes">
        <g:message code="cannedKey.featureTypes.label" default="Feature Types"/>

    </label>
    <g:select name="featureTypes" from="${org.bbop.apollo.FeatureType.list()}" multiple="multiple" optionKey="id"
              size="10" value="${cannedKeyInstance?.featureTypes*.id}" class="many-to-many" optionValue="display"/>

</div>

%{--<div class="fieldcontain ${hasErrors(bean: cannedKeyInstance, field: 'values', 'error')} ">--}%
    %{--<label for="values">--}%
        %{--<g:message code="cannedKey.values.label" default="Values"/>--}%

    %{--</label>--}%
    %{--<g:select name="values" from="${org.bbop.apollo.CannedValue.list()}" multiple="multiple" optionKey="id" size="5" optionValue="label"--}%
              %{--value="${cannedKeyInstance?.values*.id}" class="many-to-many"/>--}%

%{--</div>--}%

