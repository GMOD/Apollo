package org.bbop.apollo.gwt.shared;

/**
 * Created by ndunn on 4/19/16.
 */
public class ColorGenerator {

    public static String getColorForIndex(int i ){
        switch (i){
            case 0: return "green";
            case 1: return "blue";
            case 2: return "brown";
            case 3: return "red";
            default: return "gray";
        }
    }
}
