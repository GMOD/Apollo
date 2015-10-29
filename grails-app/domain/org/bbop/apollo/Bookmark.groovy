package org.bbop.apollo


class Bookmark {


    String type
    Integer padding
    String payload
    String sequenceList
    Organism organism
    Integer start
    Integer end

    static constraints = {
        sequenceList nullable: false
        type nullable: true
        padding nullable: true
        payload nullable: true
        organism nullable: false
        start nullable: false
        end nullable: false
    }

    static mapping = {
        sequenceList type: "text"
        payload type: "text"
        start column: "startbp"
        end column: "endbp"
    }

}
