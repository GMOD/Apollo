package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
//        defaultSequence nullable: true, blank: false
        sequence nullable: true, blank: false
        startbp nullable: true, blank: false
        endbp nullable: true, blank: false
    }

    Organism organism
//    String defaultSequence
    Boolean currentOrganism
    Sequence sequence
    Integer startbp
    Integer endbp

}
