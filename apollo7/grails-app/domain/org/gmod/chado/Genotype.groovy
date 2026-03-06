package org.gmod.chado

class Genotype {

    String name
    String uniquename
    String description

    static hasMany = [featureGenotypes                  : FeatureGenotype,
                      genotypeprops                     : Genotypeprop,
//                      ndExperimentGenotypes             : NdExperimentGenotype,
                      phendescs                         : Phendesc,
                      phenotypeComparisonsForGenotype1Id: PhenotypeComparison,
                      phenotypeComparisonsForGenotype2Id: PhenotypeComparison,
                      phenstatements                    : Phenstatement
//                      stockGenotypes                    : StockGenotype
    ]

    // TODO you have multiple hasMany references for class(es) [PhenotypeComparison]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [phenotypeComparisonsForGenotype1Id: "genotype1",
                       phenotypeComparisonsForGenotype2Id: "genotype2"]

    static mapping = {
        datasource "chado"
        id column: "genotype_id", generator: "increment"
        version false
    }

    static constraints = {
        name nullable: true
        uniquename unique: true
        description nullable: true
    }
}
