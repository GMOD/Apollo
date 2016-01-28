package org.gmod.chado

class FeatureRelationship {

    String value
    Integer rank
    Feature featureByObjectId
    Feature featureBySubjectId
    Cvterm cvterm

    static hasMany = [featureRelationshipPubs : FeatureRelationshipPub,
                      featureRelationshipprops: FeatureRelationshipprop]
    static belongsTo = [Cvterm, Feature]

    static mapping = {
        datasource "chado"
        id column: "feature_relationship_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "object_id", "subject_id"]
        rank unique: ["cvterm", "featureByObjectId", "featureBySubjectId"]
    }
}
