package org.bbop.apollo.gwt.shared.provenance;

public class WithOrFrom {

    private String prefix;
    private String lookupId;

    public WithOrFrom(String prefix, String lookup) {
        this.prefix = prefix;
        this.lookupId = lookup;
    }

    public WithOrFrom(String lookup) {
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

    public String getDisplay() {
        return prefix + ":" + lookupId;
    }
}
