package org.gmod.chado

class CvtermRelationship {

    Cvterm subject
    Cvterm object
    Cvterm type

    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cvterm_relationship_id", generator: "increment"
        version false
    }
}
