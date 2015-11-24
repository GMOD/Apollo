package org.bbop.apollo


class Bookmark {


    String projection
    Integer padding
    String payload
    String sequenceList // JSON array of sequence list
    String referenceTrack  // JSON array of reference tracks or single string
    Organism organism
    User user
    Integer start
    Integer end

    static constraints = {
        projection nullable: true
        sequenceList nullable: false
        referenceTrack nullable: true
        padding nullable: true
        payload nullable: true
        organism nullable: false
        start nullable: false
        end nullable: false
        user nullable: false
    }

    static mapping = {
        sequenceList type: "text"
        referenceTrack type: "text"
        payload type: "text"
        start column: "startbp"
        end column: "endbp"
    }

}
