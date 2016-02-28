package org.gmod.chado

class FeatureDbxref {

    Boolean isCurrent
    Feature feature
    Dbxref dbxref

    static belongsTo = [Dbxref, Feature]

    static mapping = {
        datasource "chado"
        id column: "feature_dbxref_id", generator: "increment"
        version false
    }
}
