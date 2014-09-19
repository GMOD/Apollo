package org.bbop.apollo

class User {
    String username
    String passwordHash
    
    static hasMany = [ roles: Role, permissions: String , tracks: Track,groupAnnotations: GroupAnnotation, userGroups:UserGroup]

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
