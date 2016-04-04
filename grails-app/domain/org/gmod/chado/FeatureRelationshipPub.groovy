package org.gmod.chado

class FeatureRelationshipPub {

    Pub pub
    FeatureRelationship featureRelationship

    static belongsTo = [FeatureRelationship, Pub]

    static mapping = {
        datasource "chado"
        id column: "feature_relationship_pub_id", generator: "increment"
        version false
    }
}
