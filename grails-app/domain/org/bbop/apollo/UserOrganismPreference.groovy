package org.bbop.apollo

class UserOrganismPreference extends UserPreference{

    static constraints = {
        organism nullable: false
        defaultSequence nullable: true, blank: false
    }

    Organism organism

    String defaultSequence

}
