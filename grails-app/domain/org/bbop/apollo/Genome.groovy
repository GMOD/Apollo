package org.bbop.apollo

class Genome {

    static auditable = true

    static constraints = {
        directory nullable: true
    }

    static hasMany = [
            sequences: Sequence
    ]

    String name
    String directory
}
