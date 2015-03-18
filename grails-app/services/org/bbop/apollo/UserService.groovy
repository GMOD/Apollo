package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class UserService {

    static String USER = "USER"
    static String ADMIN = "ADMIN"

//    def addOwner(Feature feature,) {
//
//    }
   
    // return admin role or user role
    Role getHighestRole(User user){
        for(Role role in user.roles.sort(){ a,b -> b.name<=>a.name }){
            return role
        }
    }
}
