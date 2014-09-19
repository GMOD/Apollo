package org.bbop.apollo

class GroupAnnotation {

    static constraints = {
    }

    static hasMany = [
            tracks: Track
            ,annotations: Annotation
    ]

    static belongsTo = [Track]

    String name
}
