package org.bbop.apollo

class UserGroup implements JsonMetadata{

    static constraints = {
        metadata(display: false, blank: true,nullable: true)
    }

    static hasMany = [
            users: User,
            admin: User
    ]

    static mapping = {
        metadata type: "text"
    }

    String name
    String metadata
    boolean publicGroup = false
}
