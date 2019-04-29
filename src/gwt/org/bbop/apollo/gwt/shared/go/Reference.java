package org.bbop.apollo.gwt.shared.go;

public class Reference {

    String prefix;
    String lookupId;


    public Reference(String lookup) {
        this.prefix = lookup.split(":")[0];
        this.lookupId = lookup.split(":")[1];
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLookupId() {
        return lookupId;
    }

    public void setLookupId(String lookupId) {
        this.lookupId = lookupId;
    }

    public String getRefereneString() {
        return prefix + ":" + lookupId;
    }
//
//    public void setRefereneString(String refereneString) {
//        this.refereneString = refereneString;
//    }
}
