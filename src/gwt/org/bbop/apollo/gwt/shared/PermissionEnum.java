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
public enum PermissionEnum implements Comparable<PermissionEnum> {

    NONE(0, 0),
    READ(1, 10),
    EXPORT(7, 30),
    WRITE(3, 50),
    ADMINISTRATE(15, 70),
    ALL_ORGANISM_ADMIN(100, 100);

    private Integer value; // pertains to the 1.0 value
    private Integer rank;


    PermissionEnum(int oldValue, int rank) {
        this.value = oldValue;
        this.rank = rank;
    }

    public String getDisplay() {
        return name().toLowerCase();
    }


    public static PermissionEnum getValueForString(String input) {
        for (PermissionEnum permissionEnum : values()) {
            if (permissionEnum.name().equals(input))
                return permissionEnum;
        }
        return null;
    }

    public static PermissionEnum getValueForOldInteger(Integer input) {
        for (PermissionEnum permissionEnum : values()) {
            if (permissionEnum.value.equals(input))
                return permissionEnum;
        }
        return null;
    }

    public static List<PermissionEnum> getValueForArray(List<String> inputs) {
        List<PermissionEnum> permissionEnumList = new ArrayList<>();
        for (String input : inputs) {
            permissionEnumList.add(getValueForString(input));
        }
        return permissionEnumList;
    }

    public Integer getValue() {
        return value;
    }

    public Integer getRank() {
        return rank;
    }

    //    @Override
//    public int compareTo(PermissionEnum o) {
//        return value - o.getValue();
//    }
}
