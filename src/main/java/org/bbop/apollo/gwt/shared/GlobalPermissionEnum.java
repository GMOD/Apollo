package org.bbop.apollo.gwt.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * The "value" is mapping for Apollo 1
 * Relateive ranks are for allowing bulk accesses.   For that WRITE access will have additional permissions (including export).
 * We will likely be adding additional permissions, as well.
 * <p>
 * Created by ndunn on 3/31/15.
 */
public enum GlobalPermissionEnum implements Comparable<GlobalPermissionEnum> {

    USER("user",10),
    INSTRUCTOR("instructor",50),
    ADMIN("admin",100);

    private String display; // pertains to the 1.0 value
    private Integer rank;


    GlobalPermissionEnum(String display , int rank) {
        this.display = display;
        this.rank = rank;
    }

    public String getLookupKey(){
        return name().toLowerCase();
    }

    public String getDisplay() {
        return display;
    }

    public static GlobalPermissionEnum getValueForString(String input) {
        for (GlobalPermissionEnum permissionEnum : values()) {
            if (permissionEnum.name().equals(input))
                return permissionEnum;
        }
        return null;
    }


    public static List<GlobalPermissionEnum> getValueForArray(List<String> inputs) {
        List<GlobalPermissionEnum> permissionEnumList = new ArrayList<>();
        for (String input : inputs) {
            permissionEnumList.add(getValueForString(input));
        }
        return permissionEnumList;
    }



    public Integer getRank() {
        return rank;
    }

    //    @Override
//    public int compareTo(PermissionEnum o) {
//        return value - o.getValue();
//    }
}
