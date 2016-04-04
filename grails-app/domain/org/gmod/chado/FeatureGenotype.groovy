package org.gmod.chado

class FeatureGenotype {

    Integer rank
    Integer cgroup
    Feature feature
    Genotype genotype
    Cvterm cvterm
    Feature chromosome

    static belongsTo = [Cvterm, Feature, Genotype]

    static mapping = {
        datasource "chado"
        id column: "feature_genotype_id", generator: "increment"
        version false
    }

    static constraints = {
//		cgroup unique: ["rank", "chromosome_id", "cvterm_id", "genotype_id", "feature_id"]
        cgroup unique: ["rank", "chromosome", "cvterm", "genotype", "feature"]
    }
}
