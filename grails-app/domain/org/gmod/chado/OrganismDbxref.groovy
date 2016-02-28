package org.gmod.chado

class OrganismDbxref {

    Dbxref dbxref
    Organism organism

    static belongsTo = [Dbxref, Organism]

    static mapping = {
        datasource "chado"
        id column: "organism_dbxref_id", generator: "increment"
        version false
    }
}
