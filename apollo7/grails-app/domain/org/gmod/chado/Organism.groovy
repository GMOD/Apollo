package org.gmod.chado

class Organism {

    String abbreviation
    String genus
    String species
    String commonName
    String comment

    static hasMany = [
//            analysisOrganisms   : AnalysisOrganism,
//	                  biomaterials: Biomaterial,
//	                  cellLines: CellLine,
                      features            : Feature,
//                      libraries           : Library,
                      organismDbxrefs     : OrganismDbxref,
                      organismprops       : Organismprop,
                      phenotypeComparisons: PhenotypeComparison,
                      phylonodeOrganisms  : PhylonodeOrganism,
//                      stocks              : Stock
    ]

    static mapping = {
        datasource "chado"
        id column: "organism_id", generator: "increment"
        version false
    }

    static constraints = {
        abbreviation nullable: true
        species unique: ["genus"]
        commonName nullable: true
        comment nullable: true
    }
}
