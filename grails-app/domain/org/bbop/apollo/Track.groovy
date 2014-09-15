package org.bbop.apollo

class Track {

    static constraints = {
        genome nullable: true
    }

    static hasMany = [
            users:User
    ]

    static belongsTo = [User]

    String name
    Genome genome
}
