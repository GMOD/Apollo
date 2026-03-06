package org.gmod.chado

class CvtermDbxref {

    Integer isForDefinition
    Dbxref dbxref
    Cvterm cvterm

    static belongsTo = [Cvterm, Dbxref]

    static mapping = {
        datasource "chado"
        id column: "cvterm_dbxref_id", generator: "increment"
        version false
    }
}
