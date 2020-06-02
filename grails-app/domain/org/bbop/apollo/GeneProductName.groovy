package org.bbop.apollo

class GeneProductName {
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
