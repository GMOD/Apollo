package org.bbop.apollo

/**
 * Maps to CVTerm Owner, no Ontology term
 */
class User implements Ontological {


    static auditable = true

    // TODO: username should be mapped to "value" of FeatureProperty
    String username
    String passwordHash
    String firstName
    String lastName

    static String cvTerm = "Owner"
    static String ontologyId = "Owner"

    static hasMany = [roles: Role, userGroups: UserGroup, bookmarks: Bookmark]

    static belongsTo = [
            UserGroup
    ]


    static constraints = {
        username(nullable: false, blank: false, unique: true, email: true)
        passwordHash(display: false, blank: false, null: false,minSize: 5)
    }

    static mapping = {
        table "grails_user"
//        password column: "grails_password"
    }
}
