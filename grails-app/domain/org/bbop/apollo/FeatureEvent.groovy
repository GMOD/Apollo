package org.bbop.apollo

import org.bbop.apollo.history.FeatureOperation

//import org.bbop.apollo.history.FeatureOperation

class FeatureEvent {


    Date dateCreated
    Date lastUpdated
    User editor
    FeatureOperation operation
    Boolean current

    String uniqueName // from original top-level feature
    String originalJsonCommand
    String newFeaturesJsonArray
    String oldFeaturesJsonArray


    static constraints = {
        editor nullable: true
        originalJsonCommand nullable: true
        newFeaturesJsonArray nullable: true
        oldFeaturesJsonArray nullable: true
    }

    static mapping = {
        originalJsonCommand type: "text"
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
