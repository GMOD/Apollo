package org.gmod.chado

class Dbxref {

    String accession
    String description
    Serializable searchableAccession
    Db db

    static hasMany = [
//			arraydesigns: Arraydesign,
//	                  assays: Assay,
//	                  biomaterialDbxrefs: BiomaterialDbxref,
//	                  biomaterials: Biomaterial,
//	                  cellLineDbxrefs: CellLineDbxref,
cvtermDbxrefs           : CvtermDbxref,
cvterms                 : Cvterm,
dbxrefprops             : Dbxrefprop,
//	                  elements: Element,
featureCvtermDbxrefs    : FeatureCvtermDbxref,
featureDbxrefs          : FeatureDbxref,
features                : Feature,
//libraryDbxrefs          : LibraryDbxref,
//ndExperimentDbxrefs     : NdExperimentDbxref,
//ndExperimentStockDbxrefs: NdExperimentStockDbxref,
organismDbxrefs         : OrganismDbxref,
phylonodeDbxrefs        : PhylonodeDbxref,
phylotrees              : Phylotree,
//	                  protocols: Protocol,
pubDbxrefs              : PubDbxref,
//stockDbxrefs            : StockDbxref,
//stocks                  : Stock
//	                  studies: Study
    ]
    static belongsTo = [Db]

    static mapping = {
        datasource "chado"
        id column: "dbxref_id", generator: "assigned"
    }

    static constraints = {
        description nullable: true
        searchableAccession nullable: true
    }
}
