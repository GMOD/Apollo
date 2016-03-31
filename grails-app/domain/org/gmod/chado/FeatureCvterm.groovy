package org.gmod.chado

class FeatureCvterm {

    Boolean isNot
    Integer rank
    Pub pub
    Feature feature
    Cvterm cvterm

    static hasMany = [featureCvtermDbxrefs: FeatureCvtermDbxref,
                      featureCvtermPubs   : FeatureCvtermPub,
                      featureCvtermprops  : FeatureCvtermprop]
    static belongsTo = [Cvterm, Feature, Pub]

    static mapping = {
        datasource "chado"
        id column: "feature_cvterm_id", generator: "increment"
        version false
    }

    static constraints = {
//		rank unique: ["pub_id", "cvterm_id", "feature_id"]
        rank unique: ["pub", "cvterm", "feature"]
    }
}
