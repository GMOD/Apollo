package org.gmod.chado

class Phylotree {

    String name
    String comment
    Dbxref dbxref
//    Analysis analysis
    Cvterm cvterm

    static hasMany = [phylonodeRelationships: PhylonodeRelationship,
                      phylonodes            : Phylonode,
                      phylotreePubs         : PhylotreePub]
//    static belongsTo = [Analysis, Cvterm, Dbxref]
    static belongsTo = [Cvterm, Dbxref]

    static mapping = {
        datasource "chado"
        id column: "phylotree_id", generator: "assigned"
        version false
    }

    static constraints = {
        name nullable: true
        comment nullable: true
    }
}
