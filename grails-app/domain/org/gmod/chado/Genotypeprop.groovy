package org.gmod.chado

class Genotypeprop {

    String value
    Integer rank
    Genotype genotype
    Cvterm cvterm

    static belongsTo = [Cvterm, Genotype]

    static mapping = {
        datasource "chado"
        id column: "genotypeprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
        rank unique: ["cvterm", "genotype"]
    }
}
