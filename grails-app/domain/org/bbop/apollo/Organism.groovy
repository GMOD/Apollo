package org.bbop.apollo

class Organism {

    static auditable = true

    static constraints = {
        comment nullable: true
        abbreviation nullable: true
        species nullable: true
        genus nullable: true
        valid nullable: true
    }

//     Integer organismId;
     String abbreviation;
     String genus;
     String species;
     String commonName;
     String comment;
     Boolean valid

    String directory

    static hasMany = [
            organismProperties: OrganismProperty
            ,organismDBXrefs: OrganismDBXref
            ,sequences: Sequence
            ,userPermissions: UserOrganismPermission
            ,groupPermissions: GroupOrganismPermission
    ]

    public String getTrackList(){
        if(!directory){
            return null
        }
        else{
            return directory + "/trackList.json"
        }
    }

    public String getRefseqFile(){
        if(!directory){
          return null
        }
        else{
            return directory + "/seq/refSeqs.json"
        }
    }
}
