package org.bbop.apollo

import org.bbop.apollo.history.FeatureOperation

class FeatureTransaction {


    Feature feature
    FeatureOperation operation
    Boolean current

    static constraints = {
    }

    static hasMany = [
            newFeatures: Feature
            ,oldFeatures: Feature
    ]

}
