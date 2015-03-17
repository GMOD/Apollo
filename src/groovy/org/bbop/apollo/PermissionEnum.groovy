package org.bbop.apollo

/**
 * Created by ndunn on 3/13/15.
 */
enum PermissionEnum implements Comparable{
    // from here:
//    public final static int NONE = 0x0;
//    public final static int READ = 0x1;
//    public final static int WRITE = 0x2;
//    public final static int PUBLISH = 0x4;
//    public final static int USER_MANAGER = 0x8;
//    public final static int ADMIN = USER_MANAGER;

    NONE(0),
    READ(1),
    EXPORT(3), // doesn't map, but we can create a scope this way
    WRITE(8),
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