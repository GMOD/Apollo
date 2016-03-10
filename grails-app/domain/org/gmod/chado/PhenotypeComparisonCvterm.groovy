package org.gmod.chado

class PhenotypeComparisonCvterm {

    Integer rank
    Pub pub
    PhenotypeComparison phenotypeComparison
    Cvterm cvterm

    static belongsTo = [Cvterm, PhenotypeComparison, Pub]

    static mapping = {
        datasource "chado"
        id column: "phenotype_comparison_cvterm_id", generator: "increment"
        version false
    }
}
