package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
//        defaultSequence nullable: true, blank: false
        sequence nullable: true, blank: false
        min nullable: true, blank: false
        max nullable: true, blank: false
    }

    Organism organism
//    String defaultSequence
    Boolean currentOrganism
    Sequence sequence
    Integer min
    Integer max

}
