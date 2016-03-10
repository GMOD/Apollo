package org.gmod.chado

class FeatureRelationship {

    String value
    Integer rank
    Feature object
    Feature subject
    Cvterm type

    static hasMany = [featureRelationshipPubs : FeatureRelationshipPub,
                      featureRelationshipprops: FeatureRelationshipprop]
    static belongsTo = [Cvterm, Feature]

    static mapping = {
        datasource "chado"
        id column: "feature_relationship_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "object_id", "subject_id"]
        rank unique: ["type", "object", "subject"]
    }
}
