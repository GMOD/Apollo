package org.bbop.apollo

class SequenceCache {

    String sequenceName
    String organismName
    String type

    Long fmin
    Long fmax
    String featureName
    String paramMap

    String response // JSON response

    static constraints = {
        sequenceName nullable: false, blank: false
        organismName nullable: false, blank: false
        type nullable: true, blank: false

        fmin nullable: true
        fmax nullable: true
        featureName nullable: true
        paramMap nullable: true, blank: true
    }

    static mapping = {
        response type: "text"
        sequenceName type: "text"
        paramMap type: "text"
    }
}
