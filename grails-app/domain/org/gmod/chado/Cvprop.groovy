package org.gmod.chado

class Cvprop {

    String value
    Integer rank
    Cv cv
    Cvterm cvterm

    static belongsTo = [Cv, Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cvprop_id", generator: "assigned"
        version false
    }

    static constraints = {
        value nullable: true
//		rank unique: ["type_id", "cv_id"]
        rank unique: ["cvterm", "cv"]
    }
}
