package org.gmod.chado

class FeatureCvtermprop {

    String value
    Integer rank
    FeatureCvterm featureCvterm
    Cvterm cvterm

    static belongsTo = [Cvterm, FeatureCvterm]

    static mapping = {
        datasource "chado"
        id column: "feature_cvtermprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "feature_cvterm_id"]
        rank unique: ["cvterm", "featureCvterm"]
    }
}
