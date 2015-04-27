package org.bbop.apollo

//import org.bbop.apollo.history.FeatureOperation

class FeatureEvent {


    Date dateCreated
    Date lastUpdated
    User editor
//    Feature feature // parent
    Long featureId
    String operation
    Boolean current

    static constraints = {
        editor nullable: true
    }

//    static hasOne = [
//            feature : Feature
//    ]

//    static belongsTo = [
//            feature: Feature
//    ]

//    static hasMany = [
//            newFeatures: Feature
//            ,oldFeatures: Feature
//    ]

}
