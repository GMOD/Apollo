package org.bbop.apollo

abstract class Permission {

    // list of permissions as JSON
    String permissions
    Organism organism
    String trackNames

    static constraints = {
        
    }

    static mapping = {
        trackNames type: "text"
    }


}
