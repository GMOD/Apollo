package org.bbop.apollo

class Organism {

    static auditable = true

    static constraints = {
        directory nullable: true
        comment nullable: true

    }

//     Integer organismId;
     String abbreviation;
     String genus;
     String species;
     String commonName;
     String comment;

    String directory

    static hasMany = [
            organismProperties: OrganismProperty
            ,organismDBXrefs: OrganismDBXref
            ,sequences: Sequence
    ]
}
