package org.bbop.apollo

class CannedValueOrganismFilter extends OrganismFilter {

    CannedValue cannedValue

    static constraints = {
        cannedValue nullable: false
    }
}
