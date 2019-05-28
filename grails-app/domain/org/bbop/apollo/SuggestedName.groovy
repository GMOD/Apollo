package org.bbop.apollo

class SuggestedName {

    static constraints = {
        name nullable: false
        metadata nullable: true
    }

    String name
    String metadata

    static hasMany = [
            featureTypes: FeatureType
    ]
}
