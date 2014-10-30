package org.bbop.apollo

class GroupAnnotation {

    static constraints = {
    }

    static hasMany = [
            sequences: Sequence
            ,features: Feature
    ]

    static belongsTo = [Sequence]

    String name
}
