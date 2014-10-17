package org.bbop.apollo

class GroupAnnotation {

    static constraints = {
    }

    static hasMany = [
            sequences: Sequence
            ,annotations: Annotation
    ]

    static belongsTo = [Sequence]

    String name
}
