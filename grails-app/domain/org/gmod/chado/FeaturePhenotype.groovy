package org.gmod.chado

class FeaturePhenotype {

    Feature feature
    Phenotype phenotype

    static belongsTo = [Feature, Phenotype]

    static mapping = {
        datasource "chado"
        id column: "feature_phenotype_id", generator: "increment"
        version false
    }
}
