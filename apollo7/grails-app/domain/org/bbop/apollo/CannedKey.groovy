package org.bbop.apollo

class CannedKey {

    static constraints = {
        label nullable: false
        metadata nullable: true
    }

    String label
    String metadata

    static hasMany = [
        featureTypes: FeatureType
    ]
}
