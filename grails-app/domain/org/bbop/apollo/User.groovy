package org.bbop.apollo

/**
 * Maps to CVTerm Owner, no Ontology term
 */
class User implements Ontological, JsonMetadata{


    static auditable = true

    // TODO: username should be mapped to "value" of FeatureProperty
    String username
    String passwordHash
    String firstName
    String lastName
    String metadata // this is JSON metadata

    static String cvTerm = "Owner"
    static String ontologyId = "Owner"

    static hasMany = [roles: Role, userGroups: UserGroup, groupAdmins: UserGroup]

    static belongsTo = [
            UserGroup
    ]

    static mappedBy = [userGroups: "users", groupAdmins: "admin"]


    static constraints = {
        username(nullable: false, blank: false, unique: true)
        passwordHash(display: false, blank: false, null: false,minSize: 5)
        metadata(display: false, blank: true,nullable: true)
    }

    static mapping = {
        table "grails_user"
    }
}
