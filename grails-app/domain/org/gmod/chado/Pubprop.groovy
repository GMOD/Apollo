package org.gmod.chado

class Pubprop {

    String value
    Integer rank
    Pub pub
    Cvterm cvterm

    static belongsTo = [Cvterm, Pub]

    static mapping = {
        datasource "chado"
        id column: "pubprop_id", generator: "assigned"
        version false
    }

    static constraints = {
//		rank nullable: true, unique: ["type_id", "pub_id"]
        rank nullable: true, unique: ["cvterm", "pub"]
    }
}
