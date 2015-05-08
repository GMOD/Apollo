package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
        sequence nullable: true, blank: false
        startbp nullable: true, blank: false
        endbp nullable: true, blank: false
    }

    Organism organism
    Boolean currentOrganism
    Sequence sequence
    Integer startbp
    Integer endbp

}
