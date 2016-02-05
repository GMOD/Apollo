package org.gmod.chado

class Phenstatement {

    Pub pub
    Environment environment
    Genotype genotype
    Phenotype phenotype
    Cvterm cvterm

    static belongsTo = [Cvterm, Environment, Genotype, Phenotype, Pub]

    static mapping = {
        datasource "chado"
        id column: "phenstatement_id", generator: "assigned"
        version false
    }
}
