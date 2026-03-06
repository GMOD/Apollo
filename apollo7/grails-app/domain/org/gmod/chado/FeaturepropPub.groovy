package org.gmod.chado

class FeaturepropPub {

    Pub pub
    Featureprop featureprop

    static belongsTo = [Featureprop, Pub]

    static mapping = {
        datasource "chado"
        id column: "featureprop_pub_id", generator: "increment"
        version false
    }
}
