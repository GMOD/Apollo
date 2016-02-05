package org.gmod.chado

class Phendesc {

    String description
    Pub pub
    Environment environment
    Genotype genotype
    Cvterm cvterm

    static belongsTo = [Cvterm, Environment, Genotype, Pub]

    static mapping = {
        datasource "chado"
        id column: "phendesc_id", generator: "assigned"
        version false
    }
}
