package org.gmod.chado

class Synonym {

    String name
    String synonymSgml
//    Serializable searchableSynonymSgml
    Cvterm type

    static hasMany = [
//			cellLineSynonyms: CellLineSynonym,
featureSynonyms: FeatureSynonym
//librarySynonyms: LibrarySynonym
    ]
    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "synonym_id", generator: "sequence"
        version false
    }

    static constraints = {
//        searchableSynonymSgml nullable: true
    }
}
