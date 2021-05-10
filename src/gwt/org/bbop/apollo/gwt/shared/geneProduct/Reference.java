package org.bbop.apollo.gwt.shared.geneProduct;

public class Reference {

    private String prefix;
    private String lookupId;

    public final static String NOT_PROVIDED = "NOT_PROVIDED";
    public final static String UNKNOWN = "UNKNOWN";


    public Reference(String display) {
        assert display.contains(":");
        this.prefix = display.split(":")[0];
        this.lookupId = display.split(":")[1];
    }

    public Reference(String prefix,String id) {
        this.prefix = prefix ;
        this.lookupId = id ;
    }

    public static Reference createEmptyReference() {
        return new Reference(NOT_PROVIDED,UNKNOWN);
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

    public String getReferenceString() {
        return prefix + ":" + lookupId;
    }
//
}
