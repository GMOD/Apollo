package org.gmod.chado

class PubRelationship {

    Pub object
    Pub subject
    Cvterm cvterm

    static belongsTo = [Cvterm, Pub]

    static mapping = {
        datasource "chado"
        id column: "pub_relationship_id", generator: "increment"
        version false
    }
}
