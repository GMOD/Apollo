package org.gmod.chado

class Phylonode {

    Integer leftIdx
    Integer rightIdx
    String label
    Double distance
    Feature feature
    Phylotree phylotree
//    Phylonode phylonode
    Cvterm cvterm

    static hasMany = [phylonodeDbxrefs                  : PhylonodeDbxref,
                      phylonodeOrganisms                : PhylonodeOrganism,
                      phylonodePubs                     : PhylonodePub,
                      phylonodeRelationshipsForObjectId : PhylonodeRelationship,
                      phylonodeRelationshipsForSubjectId: PhylonodeRelationship,
                      phylonodeprops                    : Phylonodeprop,
                      phylonodes                        : Phylonode]
    static belongsTo = [Cvterm, Feature, Phylotree]

    // TODO you have multiple hasMany references for class(es) [PhylonodeRelationship]
    //      so you'll need to disambiguate them with the 'mappedBy' property:
    static mappedBy = [phylonodeRelationshipsForObjectId : "phylonodeByObjectId",
                       phylonodeRelationshipsForSubjectId: "phylonodeBySubjectId"]

    static mapping = {
        datasource "chado"
        id column: "phylonode_id", generator: "assigned"
        version false
    }

    static constraints = {
//		leftIdx unique: ["phylotree_id"]
//		rightIdx unique: ["phylotree_id"]
        leftIdx unique: ["phylotree"]
        rightIdx unique: ["phylotree"]
        label nullable: true
        distance nullable: true, scale: 17
    }
}
