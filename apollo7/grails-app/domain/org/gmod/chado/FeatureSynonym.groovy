package org.gmod.chado

class FeatureSynonym {

    Boolean isCurrent
    Boolean isInternal
    Pub pub
    Feature feature
    Synonym synonym

    static belongsTo = [Feature, Pub, Synonym]

    static mapping = {
        datasource "chado"
        id column: "feature_synonym_id", generator: "increment"
        version false
    }
}
