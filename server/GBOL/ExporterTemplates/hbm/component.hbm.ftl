
<component 
    name="${property.name}"
    class="${property.value.componentClassName}">
    <#assign metaattributable=property>
	<#include "meta.hbm.ftl">
    
<!-- TODO: handle properties and component -->    
</component>