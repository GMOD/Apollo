package org.bbop.apollo

class UserGroup {

    static constraints = {
    }

    static hasMany = [
            users: User
    ]
}
