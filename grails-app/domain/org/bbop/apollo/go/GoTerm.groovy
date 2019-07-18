package org.bbop.apollo.go

/**
 * Placeholder for a feature with a single annotaiton
 */
class GoTerm {

    //    Long id
    String name
    String prefix
    String lookupId

    GoTerm(String lookup) {
        if(lookup.contains(":")){
            this.prefix = lookup.split(":")[0]
            this.lookupId = lookup.split(":")[1]
        }
        else{
            this.name = lookup
        }
    }

    GoTerm(String lookup,String name ) {
        this.prefix = lookup.split(":")[0]
        this.lookupId = lookup.split(":")[1]
        this.name = name
    }

    String getLinkDisplay() {
        return prefix + ":" + lookupId
    }
}
