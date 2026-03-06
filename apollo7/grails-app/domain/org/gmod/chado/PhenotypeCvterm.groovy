package org.gmod.chado

class PhenotypeCvterm {

    Integer rank
    Cvterm cvterm
    Phenotype phenotype

    static belongsTo = [Cvterm, Phenotype]

    static mapping = {
        datasource "chado"
        id column: "phenotype_cvterm_id", generator: "increment"
        version false
    }

    static constraints = {
//		rank unique: ["cvterm_id", "phenotype_id"]
        rank unique: ["cvterm", "phenotype"]
    }
}
