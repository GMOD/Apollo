package org.gmod.chado

class FeaturelocPub {

    Pub pub
    Featureloc featureloc

    static belongsTo = [Featureloc, Pub]

    static mapping = {
        datasource "chado"
        id column: "featureloc_pub_id", generator: "increment"
        version false
    }
}
