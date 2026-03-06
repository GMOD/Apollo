package org.bbop.apollo

class CannedCommentOrganismFilter extends OrganismFilter {

    CannedComment cannedComment

    static constraints = {
        cannedComment nullable: false
    }
}
