package org.gmod.chado

class PhylonodeOrganism {

    Organism organism
    Phylonode phylonode

    static belongsTo = [Organism, Phylonode]

    static mapping = {
        datasource "chado"
        id column: "phylonode_organism_id", generator: "increment"
        version false
    }
}
