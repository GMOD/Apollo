package org.bbop.apollo

class UserGroup implements JsonMetadata{

    static constraints = {
    }

    static hasMany = [
            users: User
    ]

    String name
    String metadata
    boolean publicGroup = false
}
