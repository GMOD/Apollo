package org.gmod.chado

class Phenotype {

    String uniquename
    String name
    String value
    Cvterm cvtermByObservableId
    Cvterm cvtermByAttrId
    Cvterm cvtermByCvalueId
    Cvterm cvtermByAssayId

    static hasMany = [featurePhenotypes                  : FeaturePhenotype,
//                      ndExperimentPhenotypes             : NdExperimentPhenotype,
                      phenotypeComparisonsForPhenotype1Id: PhenotypeComparison,
                      phenotypeComparisonsForPhenotype2Id: PhenotypeComparison,
                      phenotypeCvterms                   : PhenotypeCvterm,
                      phenstatements                     : Phenstatement]
    static belongsTo = [Cvterm]

    // TODO you have multiple hasMany references for class(es) [PhenotypeComparison]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [phenotypeComparisonsForPhenotype1Id: "phenotypeByPhenotype1Id",
                       phenotypeComparisonsForPhenotype2Id: "phenotypeByPhenotype2Id"]

    static mapping = {
        datasource "chado"
        id column: "phenotype_id", generator: "assigned"
        version false
    }

    static constraints = {
        uniquename unique: true
        name nullable: true
        value nullable: true
    }
}
