package org.gmod.chado

class Phylotree {

    String name
    String comment
    Dbxref dbxref
//    Analysis analysis
    Cvterm type

    static hasMany = [phylonodeRelationships: PhylonodeRelationship,
                      phylonodes            : Phylonode,
                      phylotreePubs         : PhylotreePub]
//    static belongsTo = [Analysis, Cvterm, Dbxref]
    static belongsTo = [Cvterm, Dbxref]

    static mapping = {
        datasource "chado"
        id column: "phylotree_id", generator: "increment"
        version false
    }

    static constraints = {
        name nullable: true
        comment nullable: true
    }
}
