package org.bbop.apollo

class AvailableStatus {

    static constraints = {
//        label nullable: true
        value nullable: false
        selected nullable: true
    }

//    String label
    String value
    Boolean selected = false

    static hasMany = [
            featureTypes: FeatureType
    ]

}
