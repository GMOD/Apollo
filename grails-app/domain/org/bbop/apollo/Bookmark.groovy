package org.bbop.apollo


class Bookmark {


    String type
    Integer padding
    String payload
    String sequenceJsonArray

    static constraints = {
        sequenceJsonArray nullable: false
        type nullable: true
        padding nullable: true
        payload nullable: true
    }

    static mapping = {
        sequenceJsonArray type: "text"
        payload type: "text"
    }
}
