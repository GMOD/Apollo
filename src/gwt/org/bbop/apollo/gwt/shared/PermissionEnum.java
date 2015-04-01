package org.bbop.apollo.gwt.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ndunn on 3/31/15.
 */
public enum PermissionEnum implements Comparable<PermissionEnum>{

    NONE(0),
    READ(1),
    WRITE(3),
    EXPORT(7),
    ADMINISTRATE(15),
    ALL_ORGANISM_ADMIN(100);

    private Integer value ;


    PermissionEnum(int oldValue){
        this.value = oldValue;
    }

    public String getDisplay(){
        return name().toLowerCase();
    }


    public static PermissionEnum getValueForString(String input){
        for(PermissionEnum permissionEnum : values()){
            if(permissionEnum.name().equals(input))
                return permissionEnum;
        }
        return null;
    }

    public static PermissionEnum getValueForInteger(Integer input){
        for(PermissionEnum permissionEnum : values()){
            if(permissionEnum.value.equals(input))
                return permissionEnum;
        }
        return null;
    }

    public static List<PermissionEnum> getValueForArray(List<String> inputs){
        List<PermissionEnum> permissionEnumList = new ArrayList<>();
        for(String input : inputs){
            permissionEnumList.add(getValueForString(input));
        }
        return permissionEnumList;
    }

    public Integer getValue() {
        return value;
    }

//    @Override
//    public int compareTo(PermissionEnum o) {
//        return value - o.getValue();
//    }
}
