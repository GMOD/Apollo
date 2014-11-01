package org.bbop.apollo

/**
 * Maps to CVTerm Owner, no Ontology term
 */
class User extends FeatureProperty implements Ontological{


    static auditable = true

    // TODO: username should be mapped to "value" of FeatureProperty
    String username
    String passwordHash

    String cvTerm = "Owner"

    static hasMany = [ roles: Role, permissions: String , sequences: Sequence,groupAnnotations: GroupAnnotation, userGroups:UserGroup]

    static belongsTo = [
            UserGroup
    ]


    static constraints = {
        username(nullable: false, blank: false, unique: true,email:true)
    }

    static mapping = {
        table "grails_user"
//        password column: "grails_password"
    }
}
