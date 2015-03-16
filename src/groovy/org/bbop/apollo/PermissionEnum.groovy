package org.bbop.apollo

/**
 * Created by ndunn on 3/13/15.
 */
enum PermissionEnum {
    
    READ,
    WRITE,
    EXPORT,
    ADMINISTRATE,
    
   
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