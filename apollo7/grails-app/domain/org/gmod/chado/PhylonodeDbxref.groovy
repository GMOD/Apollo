package org.gmod.chado

class PhylonodeDbxref {

    Dbxref dbxref
    Phylonode phylonode

    static belongsTo = [Dbxref, Phylonode]

    static mapping = {
        datasource "chado"
        id column: "phylonode_dbxref_id", generator: "increment"
        version false
    }
}
