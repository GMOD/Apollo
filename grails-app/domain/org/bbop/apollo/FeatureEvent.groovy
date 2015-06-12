package org.bbop.apollo

import org.bbop.apollo.history.FeatureOperation

//import org.bbop.apollo.history.FeatureOperation

class FeatureEvent {


    Date dateCreated
    Date lastUpdated
    User editor
    FeatureOperation operation
    Boolean current

    String name  // this is the name of the top-level feature (typically gene) during this event
    String originalJsonCommand
    String newFeaturesJsonArray
    String oldFeaturesJsonArray


    String parentUniqueName // will be the same, unless split, then parent name may be different
    String parentMergeUniqueName // the name of the parent merged from
    String uniqueName // from original top-level feature
    String childUniqueName // will be the same, unless merged, then name will change
    String childSplitUniqueName // on a split, then the name will change

    static constraints = {
        editor nullable: true
        originalJsonCommand nullable: true
        newFeaturesJsonArray nullable: true
        oldFeaturesJsonArray nullable: true
        name nullable: false, blank: false

        uniqueName nullable: false, blank: false
        parentUniqueName nullable: true, blank: false
        parentMergeUniqueName nullable: true, blank: false
        childUniqueName nullable: true, blank: false
        childSplitUniqueName  nullable: true, blank: false
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
