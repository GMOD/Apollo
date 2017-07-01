package org.bbop.apollo

class CannedCommentOrganismFilter {

    Organism organism
    CannedComment cannedComment

    static constraints = {
        organism nullable: false
        cannedComment nullable: false
    }
}
