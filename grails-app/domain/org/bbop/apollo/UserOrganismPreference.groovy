package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
        nativeTrackList nullable: true
        sequence nullable: true, blank: false
        startbp nullable: true, blank: false
        endbp nullable: true, blank: false
        token nullable: true, blank: false
    }

    Organism organism
    Boolean currentOrganism
    Boolean nativeTrackList
    Sequence sequence
    Integer startbp
    Integer endbp
    String token
    String clientToken
}
