package org.bbop.apollo

class Organism {

    static auditable = true

    static constraints = {
        comment nullable: true
        abbreviation nullable: true
        species nullable: true
        genus nullable: true

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

    public String getRefseqFile(){
        if(!directory){
          return null
        }
        else{
            return directory + "/seq/refSeqs.json"
        }
    }
}
