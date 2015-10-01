package org.bbop.apollo


class Bookmark {


    String type
    Integer padding
    String payload
    String sequenceList

    static constraints = {
        sequenceList nullable: false
        type nullable: true
        padding nullable: true
        payload nullable: true
    }

    static mapping = {
        sequenceList type: "text"
        payload type: "text"
    }
}
