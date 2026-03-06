package org.gmod.chado

class Featurepos {

    Double mappos
    Feature feature
    Featuremap featuremap
    Feature mapFeature

    static belongsTo = [Feature, Featuremap]

    static mapping = {
        datasource "chado"
        id column: "featurepos_id", generator: "increment"
        version false
    }

    static constraints = {
        mappos scale: 17
    }
}
