package org.gmod.chado

class PhylonodeRelationship {

    Integer rank
    Phylotree phylotree
    Phylonode object
    Phylonode subject
    Cvterm type

    static belongsTo = [Cvterm, Phylonode, Phylotree]

    static mapping = {
        datasource "chado"
        id column: "phylonode_relationship_id", generator: "increment"
        version false
    }

    static constraints = {
        rank nullable: true
    }
}
