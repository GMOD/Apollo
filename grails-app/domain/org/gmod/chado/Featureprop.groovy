package org.gmod.chado

class Featureprop {

    String value
    Integer rank
    Feature feature
    Cvterm type

    static hasMany = [featurepropPubs: FeaturepropPub]
    static belongsTo = [Cvterm, Feature]

    static mapping = {
        datasource "chado"
        id column: "featureprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "feature_id"]
        rank unique: ["type", "feature"]
    }
}
