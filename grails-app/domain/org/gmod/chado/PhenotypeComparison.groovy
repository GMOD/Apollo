package org.gmod.chado

class PhenotypeComparison {

    Pub pub
    Environment environment1
    Environment environment2
    Organism organism
    Phenotype phenotype1
    Phenotype phenotype2
    Genotype genotype1
    Genotype genotype2

    static hasMany = [phenotypeComparisonCvterms: PhenotypeComparisonCvterm]
    static belongsTo = [Environment, Genotype, Organism, Phenotype, Pub]

    static mapping = {
        datasource "chado"
        id column: "phenotype_comparison_id", generator: "increment"
        version false
    }
}
