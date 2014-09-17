package org.bbop.apollo

class Group {

    static constraints = {
    }

    static hasMany = [
            tracks: Track
    ]

    static belongsTo = [Track]

    String name
}
