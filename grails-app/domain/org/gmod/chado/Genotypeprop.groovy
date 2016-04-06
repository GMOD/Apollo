package org.gmod.chado

class Genotypeprop {
    // Note: This domain class is not part of any Chado module
    // Probably an artifact of the reverse engineering script.
    // TODO: Remove if not needed.
    String value
    Integer rank
    Genotype genotype
    Cvterm cvterm

    static belongsTo = [Cvterm, Genotype]

    static mapping = {
        datasource "chado"
        id column: "genotypeprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
        rank unique: ["cvterm", "genotype"]
    }
}
