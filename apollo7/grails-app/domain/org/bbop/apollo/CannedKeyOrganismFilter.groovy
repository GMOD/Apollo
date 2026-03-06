package org.bbop.apollo

class CannedKeyOrganismFilter extends OrganismFilter {

    CannedKey cannedKey

    static constraints = {
        cannedKey nullable: false
    }
}
