package org.gmod.chado

class Cvterm {

    String name
    String definition
    Integer isObsolete
    Integer isRelationshiptype
    Dbxref dbxref
    Cv cv

    static hasMany = [
//			acquisitionRelationships: AcquisitionRelationship,
//	                  acquisitionprops: Acquisitionprop,
//analysisfeatureprops            : Analysisfeatureprop,
//analysisprops                   : Analysisprop,
//	                  arraydesignprops: Arraydesignprop,
//	                  arraydesignsForPlatformtypeId: Arraydesign,
//	                  arraydesignsForSubstratetypeId: Arraydesign,
//	                  assayprops: Assayprop,
//	                  biomaterialRelationships: BiomaterialRelationship,
//	                  biomaterialTreatments: BiomaterialTreatment,
//	                  biomaterialprops: Biomaterialprop,
//	                  cellLineCvtermprops: CellLineCvtermprop,
//	                  cellLineCvterms: CellLineCvterm,
//	                  cellLineRelationships: CellLineRelationship,
//	                  cellLineprops: CellLineprop,
chadoprops                      : Chadoprop,
//contactRelationships            : ContactRelationship,
//contacts                        : Contact,
//	                  controls: Control,
cvprops                         : Cvprop,
cvtermDbxrefs                   : CvtermDbxref,
cvtermRelationshipsForObjectId  : CvtermRelationship,
cvtermRelationshipsForSubjectId : CvtermRelationship,
cvtermRelationshipsForTypeId    : CvtermRelationship,
cvtermpathsForObjectId          : Cvtermpath,
cvtermpathsForSubjectId         : Cvtermpath,
cvtermpathsForTypeId            : Cvtermpath,
cvtermpropsForCvtermId          : Cvtermprop,
cvtermpropsForTypeId            : Cvtermprop,
cvtermsynonymsForCvtermId       : Cvtermsynonym,
cvtermsynonymsForTypeId         : Cvtermsynonym,
dbxrefprops                     : Dbxrefprop,
//	                  elementRelationships: ElementRelationship,
//	                  elementresultRelationships: ElementresultRelationship,
//	                  elements: Element,
environmentCvterms              : EnvironmentCvterm,
expressionCvtermprops           : ExpressionCvtermprop,
expressionCvtermsForCvtermId    : ExpressionCvterm,
expressionCvtermsForCvtermTypeId: ExpressionCvterm,
expressionprops                 : Expressionprop,
featureCvtermprops              : FeatureCvtermprop,
featureCvterms                  : FeatureCvterm,
featureExpressionprops          : FeatureExpressionprop,
featureGenotypes                : FeatureGenotype,
featurePubprops                 : FeaturePubprop,
featureRelationshipprops        : FeatureRelationshipprop,
featureRelationships            : FeatureRelationship,
featuremaps                     : Featuremap,
featureprops                    : Featureprop,
features                        : Feature,
genotypeprops                   : Genotypeprop,
genotypes                       : Genotype,
//libraries                       : Library,
//libraryCvterms                  : LibraryCvterm,
//libraryprops                    : Libraryprop,
//ndExperimentStockprops          : NdExperimentStockprop,
//ndExperimentStocks              : NdExperimentStock,
//ndExperimentprops               : NdExperimentprop,
//ndExperiments                   : NdExperiment,
//ndGeolocationprops              : NdGeolocationprop,
//ndProtocolReagents              : NdProtocolReagent,
//ndProtocolprops                 : NdProtocolprop,
//ndProtocols                     : NdProtocol,
//ndReagentRelationships          : NdReagentRelationship,
//ndReagentprops                  : NdReagentprop,
//ndReagents                      : NdReagent,
organismprops                   : Organismprop,
phendescs                       : Phendesc,
phenotypeComparisonCvterms      : PhenotypeComparisonCvterm,
phenotypeCvterms                : PhenotypeCvterm,
phenotypesForAssayId            : Phenotype,
phenotypesForAttrId             : Phenotype,
phenotypesForCvalueId           : Phenotype,
phenotypesForObservableId       : Phenotype,
phenstatements                  : Phenstatement,
phylonodeRelationships          : PhylonodeRelationship,
phylonodeprops                  : Phylonodeprop,
phylonodes                      : Phylonode,
phylotrees                      : Phylotree,
//projectRelationships            : ProjectRelationship,
//projectprops                    : Projectprop,
//	                  protocolparamsForDatatypeId: Protocolparam,
//	                  protocolparamsForUnittypeId: Protocolparam,
//	                  protocols: Protocol,
pubRelationships                : PubRelationship,
pubprops                        : Pubprop,
pubs                            : Pub,
//	                  quantificationRelationships: QuantificationRelationship,
//	                  quantificationprops: Quantificationprop,
//stockCvtermprops                : StockCvtermprop,
//stockCvterms                    : StockCvterm,
//stockDbxrefprops                : StockDbxrefprop,
//stockRelationshipCvterms        : StockRelationshipCvterm,
//stockRelationships              : StockRelationship,
//stockcollectionprops            : Stockcollectionprop,
//stockcollections                : Stockcollection,
//stockprops                      : Stockprop,
//stocks                          : Stock,
//	                  studydesignprops: Studydesignprop,
//	                  studyfactors: Studyfactor,
//	                  studypropFeatures: StudypropFeature,
//	                  studyprops: Studyprop,
synonyms                        : Synonym
//	                  treatments: Treatment
    ]
    static belongsTo = [Cv, Dbxref]

    // TODO you have multiple hasMany references for class(es) [Arraydesign, CvtermRelationship, Cvtermpath, Cvtermprop, Cvtermsynonym, ExpressionCvterm, Phenotype, Protocolparam]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [
//			arraydesignsForPlatformtypeId: "TODO",
//	                   arraydesignsForSubstratetypeId: "TODO",
cvtermRelationshipsForObjectId  : "object",
cvtermRelationshipsForSubjectId : "subject",
cvtermRelationshipsForTypeId    : "type",
cvtermpathsForObjectId          : "object",
cvtermpathsForSubjectId         : "subject",
cvtermpathsForTypeId            : "type",
cvtermpropsForCvtermId          : "cvterm",
cvtermpropsForTypeId            : "type",
cvtermsynonymsForCvtermId       : "cvterm",
cvtermsynonymsForTypeId         : "type",
expressionCvtermsForCvtermId    : "cvterm",
//expressionCvtermsForCvtermTypeId: "cvtermByCvtermTypeId",
phenotypesForAssayId            : "assay",
phenotypesForAttrId             : "attr",
phenotypesForCvalueId           : "cvalue",
phenotypesForObservableId       : "observable"
//	                   protocolparamsForDatatypeId: "TODO",
//	                   protocolparamsForUnittypeId: "TODO"
    ]

    static mapping = {
        datasource "chado"
        id column: "cvterm_id", generator: "increment"
        version false
    }

    static constraints = {
        name maxSize: 1024
        definition nullable: true
//		isObsolete unique: ["cv_id", "name"]
//        isObsolete unique: ["cv", "name"]
    }
}
