package org.bbop.apollo.gwt.shared.go;

public enum Aspect {

    BP("biological process"),
    MF("molecular function"),
    CC("cellular component"),
    ;

    private String lookup;

    private Aspect(String lookupValue){
        this.lookup = lookupValue ;
    }

    public String getLookup() {
        return lookup;
    }
}
