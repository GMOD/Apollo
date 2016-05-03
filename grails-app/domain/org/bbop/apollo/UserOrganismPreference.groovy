package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
        nativeTrackList nullable: true
        bookmark nullable: true, blank: false
        startbp nullable: true, blank: false
        endbp nullable: true, blank: false
    }

    Organism organism
    Boolean currentOrganism  // this means the "active" client token
    Boolean nativeTrackList
    Bookmark bookmark
//    Sequence sequence
    Integer startbp
    Integer endbp
}
