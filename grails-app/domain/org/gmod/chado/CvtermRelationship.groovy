package org.gmod.chado

class CvtermRelationship {

    Cvterm cvtermBySubjectId
    Cvterm cvtermByObjectId
    Cvterm cvtermByTypeId

    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "cvterm_relationship_id", generator: "assigned"
        version false
    }
}
