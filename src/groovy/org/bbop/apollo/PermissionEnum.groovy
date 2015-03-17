package org.bbop.apollo

/**
 * Created by ndunn on 3/13/15.
 */
enum PermissionEnum implements Comparable{
    
    READ(1),
    WRITE(3),
    EXPORT(8), // doesn't map, but we can create a scope this way
    ADMINISTRATE(15),
  
    private Integer value 

    
//    public PermissionEnum(){
//        this.value = null
//    }
    
    public PermissionEnum(int oldValue){
        this.value = oldValue
    }
   
    public static getDisplay(){
        return this.getSimpleName().toLowerCase()
    }


    public static PermissionEnum getValueForString(String input){
        for(PermissionEnum permissionEnum in values()){
            if(permissionEnum.name()==input)
                return permissionEnum
        }
        return null 
    }

    public static List<PermissionEnum> getValueForArray(List<String> inputs){
        List<PermissionEnum> permissionEnumList = new ArrayList<>()
        for(String input in inputs){
            permissionEnumList.add(getValueForString(input))
        }
        return permissionEnumList
    }
}