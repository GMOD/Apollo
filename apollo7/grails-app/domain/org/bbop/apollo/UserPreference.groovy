package org.bbop.apollo

class UserPreference extends Preference{

    static constraints = {
        user nullable: false
    }

    User user
}
