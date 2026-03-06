package org.gmod.chado

class FeatureCvtermDbxref {

    Dbxref dbxref
    FeatureCvterm featureCvterm

    static belongsTo = [Dbxref, FeatureCvterm]

    static mapping = {
        datasource "chado"
        id column: "feature_cvterm_dbxref_id", generator: "increment"
        version false
    }
}
