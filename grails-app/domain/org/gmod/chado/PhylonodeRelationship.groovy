package org.gmod.chado

class PhylonodeRelationship {

    Integer rank
    Phylotree phylotree
    Phylonode phylonodeByObjectId
    Phylonode phylonodeBySubjectId
    Cvterm cvterm

    static belongsTo = [Cvterm, Phylonode, Phylotree]

    static mapping = {
        datasource "chado"
        id column: "phylonode_relationship_id", generator: "assigned"
        version false
    }

    static constraints = {
        rank nullable: true
    }
}
