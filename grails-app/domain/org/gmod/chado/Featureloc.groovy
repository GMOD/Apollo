package org.gmod.chado

class Featureloc {

    Integer fmin
    Boolean isFminPartial
    Integer fmax
    Boolean isFmaxPartial
    Short strand
    Integer phase
    String residueInfo
    Integer locgroup
    Integer rank
    Feature srcfeature
    Feature feature

    static hasMany = [featurelocPubs: FeaturelocPub]
    static belongsTo = [Feature]

    static mapping = {
        datasource "chado"
        id column: "featureloc_id", generator: "increment"
        version false
    }

    static constraints = {
        fmin nullable: true
        fmax nullable: true
        strand nullable: true
        phase nullable: true
        residueInfo nullable: true
//		rank unique: ["locgroup", "feature_id"]
        rank unique: ["locgroup", "feature"]
    }
}
