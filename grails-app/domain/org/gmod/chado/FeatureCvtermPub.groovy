package org.gmod.chado

class FeatureCvtermPub {

    Pub pub
    FeatureCvterm featureCvterm

    static belongsTo = [FeatureCvterm, Pub]

    static mapping = {
        datasource "chado"
        id column: "feature_cvterm_pub_id", generator: "increment"
        version false
    }
}
