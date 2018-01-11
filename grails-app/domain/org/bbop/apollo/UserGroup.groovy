package org.bbop.apollo

class UserGroup implements JsonMetadata{

    static constraints = {
        metadata(display: false, blank: true,nullable: true)
    }

    static hasMany = [
            users: User
    ]

    String name
    String metadata
    boolean publicGroup = false
}
