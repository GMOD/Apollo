package org.bbop.apollo

class CannedComment {

    static constraints = {
        comment nullable: false
        metadata nullable: true
        featureTypes nullable: true
    }

    String comment
    String featureTypes
    String metadata
}
