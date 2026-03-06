package org.gmod.chado

class Dbxrefprop {

    String value
    Integer rank
    Dbxref dbxref
    Cvterm type

    static belongsTo = [Cvterm, Dbxref]

    static mapping = {
        datasource "chado"
        id column: "dbxrefprop_id", generator: "increment"
        version false
    }

    static constraints = {
//		rank unique: ["type_id", "dbxref_id"]
        rank unique: ["type", "dbxref"]
    }
}
