package org.bbop.apollo

class Organism {

    static constraints = {
    }
     Integer organismId;
     String abbreviation;
     String genus;
     String species;
     String commonName;
     String comment;
//     Set<OrganismProperty> organismProperties = new HashSet<OrganismProperty>(0);
//     Set<OrganismDBXref> organismDBXrefs = new HashSet<OrganismDBXref>(0);

    static hasMany = [
            organismProperties: OrganismProperty
            ,organismDBXrefs: OrganismDBXref
    ]
}
