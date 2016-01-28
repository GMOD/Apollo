package org.gmod.chado

class PhenotypeComparison {

    Pub pub
    Environment environmentByEnvironment1Id
    Environment environmentByEnvironment2Id
    Organism organism
    Phenotype phenotypeByPhenotype1Id
    Phenotype phenotypeByPhenotype2Id
    Genotype genotypeByGenotype1Id
    Genotype genotypeByGenotype2Id

    static hasMany = [phenotypeComparisonCvterms: PhenotypeComparisonCvterm]
    static belongsTo = [Environment, Genotype, Organism, Phenotype, Pub]

    static mapping = {
        datasource "chado"
        id column: "phenotype_comparison_id", generator: "assigned"
        version false
    }
}
