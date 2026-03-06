package org.gmod.chado

class FeaturemapPub {

    Pub pub
    Featuremap featuremap

    static belongsTo = [Featuremap, Pub]

    static mapping = {
        datasource "chado"
        id column: "featuremap_pub_id", generator: "increment"
        version false
    }
}
