package org.gmod.chado

class Featurepos {

    Double mappos
    Feature featureByFeatureId
    Featuremap featuremap
    Feature featureByMapFeatureId

    static belongsTo = [Feature, Featuremap]

    static mapping = {
        datasource "chado"
        id column: "featurepos_id", generator: "assigned"
        version false
    }

    static constraints = {
        mappos scale: 17
    }
}
