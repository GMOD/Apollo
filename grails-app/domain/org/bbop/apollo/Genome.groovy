package org.bbop.apollo

class Genome {

    static constraints = {
        directory nullable: true
    }

    static hasMany = [
            sequences: Sequence
    ]

    String name
    String directory
}
