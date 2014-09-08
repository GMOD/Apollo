	<list
		name="${property.name}"
        inverse="${property.value.inverse?string}"
		>
		<#assign metaattributable=property>
		<#include "meta.hbm.ftl">
		
		<key> 
           <#foreach column in property.value.key.columnIterator>
              <#include "column.hbm.ftl">
           </#foreach>
        </key>
		<index column="idx"/>
		<element type="string" column="dummy"/>
	</list>


