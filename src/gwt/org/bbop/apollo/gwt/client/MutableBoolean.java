package org.bbop.apollo.gwt.client;

/**
 * Created by nathandunn on 3/21/16.
 */
public class MutableBoolean {
    private Boolean booleanValue ;

    public MutableBoolean(boolean booleanValue){
        this.booleanValue = booleanValue ;
    }


    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }
}
