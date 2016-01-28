package org.gmod.chado

class Dbxrefprop {

    String value
    Integer rank
    Dbxref dbxref
    Cvterm cvterm

    static belongsTo = [Cvterm, Dbxref]

    static mapping = {
        datasource "chado"
        id column: "dbxrefprop_id", generator: "assigned"
        version false
    }

    static constraints = {
//		rank unique: ["type_id", "dbxref_id"]
        rank unique: ["cvterm", "dbxref"]
    }
}
