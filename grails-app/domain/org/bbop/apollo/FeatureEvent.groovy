package org.bbop.apollo

//import org.bbop.apollo.history.FeatureOperation

class FeatureEvent {


    Date dateCreated
    Date lastUpdated
    User editor
    String uniqueName // from feature
    String operation
    Boolean current

    String newFeaturesJsonArray
    String oldFeaturesJsonArray

    static constraints = {
        editor nullable: true
        newFeaturesJsonArray nullable: true
        oldFeaturesJsonArray nullable: true
    }

    static mapping = {
        newFeaturesJsonArray type: "text"
        oldFeaturesJsonArray type: "text"
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
