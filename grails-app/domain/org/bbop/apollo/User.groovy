package org.bbop.apollo

/**
 * Maps to CVTerm Owner, no Ontology term
 */
class User implements Ontological {


    static auditable = true

    // TODO: username should be mapped to "value" of FeatureProperty
    String username
    String email
    String passwordHash
    String firstName
    String lastName
    String metadata // any other internal information

    static String cvTerm = "Owner"
    static String ontologyId = "Owner"

    static hasMany = [roles: Role, userGroups: UserGroup]

    static belongsTo = [
            UserGroup
    ]


    static constraints = {
        username(nullable: false, blank: false, unique: true)
        email(nullable: true, blank: true, unique: true, email: true)
        passwordHash(display: false, blank: false, null: false,minSize: 5)
        metadata(nullable: true,blank:true)
    }

    static mapping = {
        table "grails_user"
        metadata type: "text"
//        password column: "grails_password"
    }
}
