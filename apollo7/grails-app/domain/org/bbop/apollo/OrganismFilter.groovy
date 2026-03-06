package org.bbop.apollo

abstract class OrganismFilter {

    Organism organism

    static constraints = {
        organism nullable: false
    }
}
