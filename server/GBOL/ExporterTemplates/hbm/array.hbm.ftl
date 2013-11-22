<#assign value = property.value>
<#assign dependentValue = value.getKey()>
<#if c2h.isOneToMany(value.getElement())>
 <#assign toManyElement = "one-to-many">
 <#assign toManyClass = value.getElement().getAssociatedClass().getEntityName()>
<#else>
 <#-- many-to-one not valid-->
 <#assign toManyElement = "many-to-many">
<#assign toManyClass = value.getElement().getType().getAssociatedEntityName()>
</#if>
<array name="${property.name}" cascade="${property.cascade}" 
 <#assign metaattributable=property>
 <#include "meta.hbm.ftl">
<#if c2h.hasFetchMode(property)> fetch="${fetch}"</#if>>
    <key> 
       <#foreach column in dependentValue.columnIterator>
                <#include "column.hbm.ftl">
       </#foreach>
    </key>
    <list-index>
       <#foreach column in value.getIndex().getColumnIterator()>
                <#include "column.hbm.ftl">
       </#foreach>
    </list-index>
<${toManyElement} class="${toManyClass}" />
</array>