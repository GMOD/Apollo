package org.bbop.apollo

class UserOrganismPermission extends UserPermission{
    
    String permissions // JSONArray wrapping PermissionEnum
    
    static belongsTo = [ Organism ]

    static constraints = {
    }

    static mapping = {
    }
    


}
