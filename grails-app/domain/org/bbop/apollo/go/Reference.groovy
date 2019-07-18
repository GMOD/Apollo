package org.bbop.apollo.go

class Reference {

    String prefix
    String lookupId


    Reference(String lookup) {
        this.prefix = lookup.split(":")[0]
        this.lookupId = lookup.split(":")[1]
    }

    String getReferenceString() {
        return prefix + ":" + lookupId
    }
}
