package org.bbop.apollo

/**
 *  This is just a decorator over the general Preference class for global, application-level preferences
 */
class ApplicationPreference {

    static constraints = {
        name nullable: false ,blank: false, unique: true
        value nullable: true , blank: true
    }

    String name
    String value
}
