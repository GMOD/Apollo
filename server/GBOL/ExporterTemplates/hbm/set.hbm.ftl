	<set
		name="${property.name}"
		inverse="${property.value.inverse?string}"
		<#if !c2h.isOneToMany(property)>
		table="${property.value.collectionTable.name}"
		</#if>
		>
		<#assign metaattributable=property>
		<#include "meta.hbm.ftl">
		<key> 
        <#foreach column in property.value.key.columnIterator>
          <#include "column.hbm.ftl">
        </#foreach>
        </key>
<#if c2h.isOneToMany(property)>
		<one-to-many 
			 class="${property.getValue().getElement().getAssociatedClass().getClassName()}"
<#if !property.getValue().getElement().getAssociatedClass().getClassName().equals(property.getValue().getElement().getReferencedEntityName())>
			 entity-name="${property.getValue().getElement().getReferencedEntityName()}"
</#if>
			/>			
<#elseif c2h.isManyToMany(property)>
        <many-to-many 
			 entity-name="${property.getValue().getElement().getReferencedEntityName()}"> <#-- lookup needed classname -->
<#foreach column in property.getValue().getElement().columnIterator>
    <#include "column.hbm.ftl">
</#foreach>            
		</many-to-many>
</#if>
	</set>
