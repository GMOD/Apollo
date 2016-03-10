package org.gmod.chado

class Cvtermprop {

    String value
    Integer rank
    Cvterm cvterm
    Cvterm type

    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cvtermprop_id", generator: "increment"
        version false
    }

    static constraints = {
//		rank unique: ["value", "type_id", "cvterm_id"]
        rank unique: ["value", "type", "cvterm"]
    }
}
