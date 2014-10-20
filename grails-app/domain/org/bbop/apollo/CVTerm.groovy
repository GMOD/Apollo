package org.bbop.apollo

class CVTerm {

    static constraints = {
    }

    Integer cvtermId;
     CV cv;
     DBXref dbxref;
     String name;
     String definition;
     int isObsolete;
     int isRelationshipType;
//     Set<CVTermRelationship> childCVTermRelationships = new HashSet<CVTermRelationship>(0);
//     Set<CVTermRelationship> parentCVTermRelationships = new HashSet<CVTermRelationship>(0);
//     Set<CVTermPath> parentCVTermPaths = new HashSet<CVTermPath>(0);
//     Set<CVTermPath> childCVTermPaths = new HashSet<CVTermPath>(0);

    static hasMany = [
            childCVTermRelationships : CVTermRelationship
            ,parentCVTermRelationships : CVTermRelationship
            ,parentCVTermPaths: CVTermPath
            ,childCVTermPaths: CVTermPath
    ]

}
