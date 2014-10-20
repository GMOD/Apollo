package org.bbop.apollo

class Phenotype {

    static constraints = {
    }

    Integer phenotypeId;
    CVTerm attribute;
    CVTerm cvalue;
    CVTerm assay;
    CVTerm observable;
    String uniqueName;
    String value;
//    Set<PhenotypeCVTerm> phenotypeCVTerms = new HashSet<PhenotypeCVTerm>(0);
//    Set<PhenotypeStatement> phenotypeStatements = new HashSet<PhenotypeStatement>(0);
//    Set<FeaturePhenotype> featurePhenotypes = new HashSet<FeaturePhenotype>(0);

    static hasMany = [
            phenotypeCVTerms : PhenotypeCVTerm
            ,phenotypeStatements: PhenotypeStatement
            ,featurePhenotypes : FeaturePhenotype
    ]
}
