package org.gmod.chado

class Genotype {

    String name
    String uniquename
    String description
    Cvterm cvterm

    static hasMany = [featureGenotypes                  : FeatureGenotype,
                      genotypeprops                     : Genotypeprop,
//                      ndExperimentGenotypes             : NdExperimentGenotype,
                      phendescs                         : Phendesc,
                      phenotypeComparisonsForGenotype1Id: PhenotypeComparison,
                      phenotypeComparisonsForGenotype2Id: PhenotypeComparison,
                      phenstatements                    : Phenstatement
//                      stockGenotypes                    : StockGenotype
    ]
    static belongsTo = [Cvterm]

    // TODO you have multiple hasMany references for class(es) [PhenotypeComparison]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [phenotypeComparisonsForGenotype1Id: "genotypeByGenotype1Id",
                       phenotypeComparisonsForGenotype2Id: "genotypeByGenotype2Id"]

    static mapping = {
        datasource "chado"
        id column: "genotype_id", generator: "assigned"
        version false
    }

    static constraints = {
        name nullable: true
        uniquename unique: true
        description nullable: true
    }
}
