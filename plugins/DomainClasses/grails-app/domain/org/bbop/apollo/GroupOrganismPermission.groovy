package org.bbop.apollo

class GroupOrganismPermission extends GroupPermission{

    String permissions // JSONArray wrapping PermissionEnum

    static belongsTo = [ Organism, UserGroup ]

    static constraints = {
    }

    static mapping = {
    }

}
