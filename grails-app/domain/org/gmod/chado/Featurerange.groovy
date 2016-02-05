package org.gmod.chado

class Featurerange {

    String rangestr
    Feature featureByRightendfId
    Feature featureByLeftstartfId
    Feature featureByFeatureId
    Featuremap featuremap
    Feature featureByLeftendfId
    Feature featureByRightstartfId

    static belongsTo = [Feature, Featuremap]

    static mapping = {
        datasource "chado"
        id column: "featurerange_id", generator: "assigned"
        version false
    }

    static constraints = {
        rangestr nullable: true
    }
}
