package org.bbop.apollo

import org.bbop.apollo.history.FeatureOperation

class FeatureEvent {


    Date dateCreated
    Date lastUpdated
    User editor
    Feature feature
    FeatureOperation operation
    Boolean current

    static constraints = {
        editor nullable: true
    }

    static hasMany = [
            newFeatures: Feature
            ,oldFeatures: Feature
    ]

}
