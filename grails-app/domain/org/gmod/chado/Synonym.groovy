package org.gmod.chado

class Synonym {

    String name
    String synonymSgml
    Serializable searchableSynonymSgml
    Cvterm cvterm

    static hasMany = [
//			cellLineSynonyms: CellLineSynonym,
featureSynonyms: FeatureSynonym
//librarySynonyms: LibrarySynonym
    ]
    static belongsTo = [Cvterm]

    static mapping = {
        datasource "chado"
        id column: "synonym_id", generator: "assigned"
        version false
    }

    static constraints = {
        searchableSynonymSgml nullable: true
    }
}
