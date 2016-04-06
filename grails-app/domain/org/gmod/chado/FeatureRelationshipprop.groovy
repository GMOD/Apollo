package org.gmod.chado

class FeatureRelationshipprop {

    String value
    Integer rank
    FeatureRelationship featureRelationship
    Cvterm type

    static hasMany = [featureRelationshippropPubs: FeatureRelationshippropPub]
    static belongsTo = [Cvterm, FeatureRelationship]

    static mapping = {
        datasource "chado"
        id column: "feature_relationshipprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "feature_relationship_id"]
        rank unique: ["type", "featureRelationship"]
    }
}
