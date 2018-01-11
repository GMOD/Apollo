package org.bbop.apollo

class UserGroup implements JsonMetadata{

    static constraints = {
        metadata nullable: true
    }

    static hasMany = [
            users: User
    ]

    String name
    String metadata
    boolean publicGroup = false
}
