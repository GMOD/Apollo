package org.bbop.apollo

class CannedValue {

    static constraints = {
        label nullable: false
        metadata nullable: true
    }

    String label
    String metadata

    static belongsTo = [
       CannedKey
    ]

    static hasMany = [
        featureTypes: FeatureType
    ]
}
