package org.bbop.apollo

class GroupOrganismPermission extends GroupPermission{

    String permissions // JSONArray wrapping PermissionEnum

    static belongsTo = [ Organism ]

    static constraints = {
    }

    static mapping = {
    }

}
