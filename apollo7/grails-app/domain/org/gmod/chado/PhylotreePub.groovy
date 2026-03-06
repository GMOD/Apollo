package org.gmod.chado

class PhylotreePub {

    Pub pub
    Phylotree phylotree

    static belongsTo = [Phylotree, Pub]

    static mapping = {
        datasource "chado"
        id column: "phylotree_pub_id", generator: "increment"
        version false
    }
}
