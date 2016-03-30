package org.bbop.apollo

class Genome {

    static auditable = true

    static constraints = {
        directory nullable: true
    }

    static hasMany = [
    ]

    String name
    String directory
}
