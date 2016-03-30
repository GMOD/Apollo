package org.bbop.apollo

class UserGroup {

    static constraints = {
    }

    static hasMany = [
            users: User
    ]

    String name
    boolean publicGroup = false
}
