package org.gmod.chado

class Cvtermpath {

    Integer pathdistance
    Cv cv
    Cvterm subject
    Cvterm object
    Cvterm type

    static belongsTo = [Cv, Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cvtermpath_id", generator: "increment"
        version false
    }

    static constraints = {
//		pathdistance nullable: true, unique: ["type_id", "object_id", "subject_id"]
        pathdistance nullable: true, unique: ["cv", "subject", "object"]
    }
}
