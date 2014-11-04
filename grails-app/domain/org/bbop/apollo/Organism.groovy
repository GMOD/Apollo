package org.bbop.apollo

class Organism {

    static constraints = {
    }

//     Integer organismId;
     String abbreviation;
     String genus;
     String species;
     String commonName;
     String comment;

    static hasMany = [
            organismProperties: OrganismProperty
            ,organismDBXrefs: OrganismDBXref
    ]
}
