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


    Long parentId // will be the same, unless split, then parent name may be different
    Long parentMergeId // the name of the parent merged from
    String uniqueName // from original top-level feature
    Long childId // will be the same, unless merged, then name will change
    Long childSplitId // on a split, then the name will change

    static constraints = {
        editor nullable: true
        originalJsonCommand nullable: true
        newFeaturesJsonArray nullable: true
        oldFeaturesJsonArray nullable: true
        name nullable: false, blank: false

        uniqueName nullable: false, blank: false
        parentId nullable: true, blank: false
        parentMergeId nullable: true, blank: false
        childId nullable: true, blank: false
        childSplitId  nullable: true, blank: false
    }

    static mapping = {
        uniqueName index: "feature_uniqueName"
        name type: "text"
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
