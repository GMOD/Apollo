package org.gmod.chado

class PubRelationship {

    Pub pubByObjectId
    Pub pubBySubjectId
    Cvterm cvterm

    static belongsTo = [Cvterm, Pub]

    static mapping = {
        datasource "chado"
        id column: "pub_relationship_id", generator: "assigned"
        version false
    }
}
