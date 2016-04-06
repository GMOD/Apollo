package org.gmod.chado

class Phendesc {

    String description
    Pub pub
    Environment environment
    Genotype genotype
    Cvterm type

    static belongsTo = [Cvterm, Environment, Genotype, Pub]

    static mapping = {
        datasource "chado"
        id column: "phendesc_id", generator: "increment"
        version false
    }
}
