package org.gmod.chado

class Cvprop {
    // Note: This domain class is not part of any Chado module
    // Probably an artifact of the reverse engineering script.
    // TODO: Remove if not needed.
    String value
    Integer rank
    Cv cv
    Cvterm type

    static belongsTo = [Cv, Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cvprop_id", generator: "increment"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "cv_id"]
        rank unique: ["type", "cv"]
    }
}
