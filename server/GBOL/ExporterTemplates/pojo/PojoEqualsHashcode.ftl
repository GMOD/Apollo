<#if pojo.needsEqualsHashCode() && !clazz.superclass?exists>   public boolean equals(Object other) {
         if ( (this == other ) ) return true;
		 if ( (other == null ) ) return false;
		 if ( !(other instanceof Abstract${pojo.getDeclarationName()}) ) return false;
		 Abstract${pojo.getDeclarationName()} castOther = ( Abstract${pojo.getDeclarationName()} ) other; 
         
		 return ${pojo.generateEquals("this", "castOther", jdk5)};
   }
   
   public int hashCode() {
         int result = 17;
         
<#foreach property in pojo.getAllPropertiesIterator()>         ${pojo.generateHashCode(property, "result", "this", jdk5)}
</#foreach>         return result;
   }   
</#if>