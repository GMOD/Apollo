package org.gmod.chado

class Pub {

    String title
    String volumetitle
    String volume
    String seriesName
    String issue
    String pyear
    String pages
    String miniref
    String uniquename
    Boolean isObsolete
    String publisher
    String pubplace
    Cvterm type

    static hasMany = [
//			cellLineCvterms: CellLineCvterm,
//	                  cellLineFeatures: CellLineFeature,
//	                  cellLineLibraries: CellLineLibrary,
//	                  cellLinePubs: CellLinePub,
//	                  cellLineSynonyms: CellLineSynonym,
//	                  cellLinepropPubs: CellLinepropPub,
expressionPubs              : ExpressionPub,
featureCvtermPubs           : FeatureCvtermPub,
featureCvterms              : FeatureCvterm,
featureExpressions          : FeatureExpression,
featurePubs                 : FeaturePub,
featureRelationshipPubs     : FeatureRelationshipPub,
featureRelationshippropPubs : FeatureRelationshippropPub,
featureSynonyms             : FeatureSynonym,
featurelocPubs              : FeaturelocPub,
featuremapPubs              : FeaturemapPub,
featurepropPubs             : FeaturepropPub,
//libraryCvterms              : LibraryCvterm,
//libraryPubs                 : LibraryPub,
//librarySynonyms             : LibrarySynonym,
//librarypropPubs             : LibrarypropPub,
//ndExperimentPubs            : NdExperimentPub,
phendescs                   : Phendesc,
phenotypeComparisonCvterms  : PhenotypeComparisonCvterm,
phenotypeComparisons        : PhenotypeComparison,
phenstatements              : Phenstatement,
phylonodePubs               : PhylonodePub,
phylotreePubs               : PhylotreePub,
//projectPubs                 : ProjectPub,
//	                  protocols: Protocol,
pubDbxrefs                  : PubDbxref,
pubRelationshipsForObjectId : PubRelationship,
pubRelationshipsForSubjectId: PubRelationship,
pubauthors                  : Pubauthor,
pubprops                    : Pubprop,
//stockCvterms                : StockCvterm,
//stockPubs                   : StockPub,
//stockRelationshipCvterms    : StockRelationshipCvterm,
//stockRelationshipPubs       : StockRelationshipPub,
//stockpropPubs               : StockpropPub
//	                  studies: Study
    ]
    static belongsTo = [Cvterm]

    // TODO you have multiple hasMany references for class(es) [PubRelationship]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [pubRelationshipsForObjectId : "object",
                       pubRelationshipsForSubjectId: "subject"]

    static mapping = {
        datasource "chado"
        id column: "pub_id", generator: "increment"
        version false
    }

    static constraints = {
        title nullable: true
        volumetitle nullable: true
        volume nullable: true
        seriesName nullable: true
        issue nullable: true
        pyear nullable: true
        pages nullable: true
        miniref nullable: true
        uniquename unique: true
        isObsolete nullable: true
        publisher nullable: true
        pubplace nullable: true
    }
}
