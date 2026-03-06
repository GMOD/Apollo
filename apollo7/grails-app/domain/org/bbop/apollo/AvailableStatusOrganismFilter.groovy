package org.bbop.apollo

class AvailableStatusOrganismFilter extends OrganismFilter {

    AvailableStatus availableStatus

    static constraints = {
        availableStatus nullable: false
    }
}
