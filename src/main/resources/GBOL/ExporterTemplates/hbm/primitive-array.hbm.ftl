<#assign value = property.value><#assign table = value.getElement().getTable().getName()><#assign dependentValue = value.getKey()>
<primitive-array name="${property.name}" table="${table}">
 <#assign metaattributable=property>
 <#include "meta.hbm.ftl">
 <key>
       <#foreach column in dependentValue.getColumnIterator()>
        <#include "column.hbm.ftl">
       </#foreach>
 </key>
 <index>
       <#foreach column in value.getIndex().getColumnIterator()>
         <#include "column.hbm.ftl">   
       </#foreach>
 </index>
 <element type="${value.getElementClass()}" >
       <#foreach column in value.getElement().getColumnIterator()>         
         <#include "column.hbm.ftl">   
       </#foreach>
 </element>
</primitive-array>