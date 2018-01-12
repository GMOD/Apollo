package org.bbop.apollo

/**
 * These are global roles.
 */
class Role {
    String name
    Integer rank

    static hasMany = [ users: User, permissions: String ]
    static belongsTo = User

    static constraints = {
        name(nullable: false, blank: false, unique: true)
        rank(nullable: true, blank: false, unique: true)
    }
}
