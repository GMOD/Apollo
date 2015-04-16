package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        currentOrganism nullable: false
        defaultSequence nullable: true, blank: false
    }

    Organism organism
    String defaultSequence
    Boolean currentOrganism = false

}
