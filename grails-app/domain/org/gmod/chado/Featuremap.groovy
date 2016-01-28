package org.gmod.chado

class Featuremap {

    String name
    String description
    Cvterm cvterm

    static hasMany = [featuremapPubs: FeaturemapPub,
                      featureposes  : Featurepos,
                      featureranges : Featurerange]
    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "featuremap_id", generator: "assigned"
        version false
    }

    static constraints = {
        name nullable: true, unique: true
        description nullable: true
    }
}
