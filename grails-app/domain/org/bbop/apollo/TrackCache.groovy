package org.bbop.apollo

class TrackCache {

    String trackName
    String sequenceName
    String organismName
    String type


    Long fmin
    Long fmax
    String featureName
    String paramMap

    String response // JSON response

    static constraints = {
        trackName nullable: false, blank: false
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
        trackName type: "text"
        sequenceName type: "text"
        paramMap type: "text"
    }
}
