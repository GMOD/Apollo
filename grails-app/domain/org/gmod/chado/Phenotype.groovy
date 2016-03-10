package org.gmod.chado

class Phenotype {

    String uniquename
    String name
    String value
    Cvterm observable
    Cvterm attr
    Cvterm cvalue
    Cvterm assay

    static hasMany = [featurePhenotypes                  : FeaturePhenotype,
//                      ndExperimentPhenotypes             : NdExperimentPhenotype,
                      phenotypeComparisonsForPhenotype1Id: PhenotypeComparison,
                      phenotypeComparisonsForPhenotype2Id: PhenotypeComparison,
                      phenotypeCvterms                   : PhenotypeCvterm,
                      phenstatements                     : Phenstatement]
    static belongsTo = [Cvterm]

    // TODO you have multiple hasMany references for class(es) [PhenotypeComparison]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [phenotypeComparisonsForPhenotype1Id: "phenotype1",
                       phenotypeComparisonsForPhenotype2Id: "phenotype2"]

    static mapping = {
        datasource "chado"
        id column: "phenotype_id", generator: "increment"
        version false
    }

    static constraints = {
        uniquename unique: true
        name nullable: true
        value nullable: true
        observable nullable: true
        attr nullable: true
        cvalue nullable: true
        assay nullable: true
    }
}
