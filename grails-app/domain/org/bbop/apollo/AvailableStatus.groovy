package org.bbop.apollo

class AvailableStatus {

    static constraints = {
//        label nullable: true
        value nullable: false
    }

//    String label
    String value

    static hasMany = [
            featureTypes: FeatureType
    ]

}
