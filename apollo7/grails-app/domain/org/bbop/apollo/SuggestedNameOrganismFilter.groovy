package org.bbop.apollo

class SuggestedNameOrganismFilter extends OrganismFilter {

    SuggestedName suggestedName

    static constraints = {
        suggestedName nullable: false
    }
}
