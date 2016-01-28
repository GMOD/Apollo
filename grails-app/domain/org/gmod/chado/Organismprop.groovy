package org.gmod.chado

class Organismprop {

    String value
    Integer rank
    Organism organism
    Cvterm cvterm

    static belongsTo = [Cvterm, Organism]

    static mapping = {
        datasource "chado"
        id column: "organismprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "organism_id"]
        rank unique: ["cvterm", "organism"]
    }
}
