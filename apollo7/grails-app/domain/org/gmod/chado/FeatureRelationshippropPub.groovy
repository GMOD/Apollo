package org.gmod.chado

class FeatureRelationshippropPub {

    Pub pub
    FeatureRelationshipprop featureRelationshipprop

    static belongsTo = [FeatureRelationshipprop, Pub]

    static mapping = {
        datasource "chado"
        id column: "feature_relationshipprop_pub_id", generator: "increment"
        version false
    }
}
