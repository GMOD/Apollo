package org.gmod.chado

class FeaturePub {

    Pub pub
    Feature feature

    static hasMany = [featurePubprops: FeaturePubprop]
    static belongsTo = [Feature, Pub]

    static mapping = {
        datasource "chado"
        id column: "feature_pub_id", generator: "increment"
        version false
    }
}
