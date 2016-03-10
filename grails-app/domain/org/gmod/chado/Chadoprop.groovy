package org.gmod.chado

class Chadoprop {
    // Note: This domain class is not part of any Chado module
    // Probably an artifact of the reverse engineering script.
    // TODO: Remove if not needed.
    String value
    Integer rank
    Cvterm cvterm

    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "chadoprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id"]
        rank unique: ["cvterm"]
    }
}
