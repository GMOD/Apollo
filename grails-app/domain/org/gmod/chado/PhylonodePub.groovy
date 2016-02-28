package org.gmod.chado

class PhylonodePub {

    Pub pub
    Phylonode phylonode

    static belongsTo = [Phylonode, Pub]

    static mapping = {
        datasource "chado"
        id column: "phylonode_pub_id", generator: "increment"
        version false
    }
}
