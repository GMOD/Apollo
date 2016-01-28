package org.gmod.chado

/**
 * An application metadata
 */
class ApplicationMetadata {

    static mapping = {
        datasource 'lookup'
    }

    static constraints = {
        key nullable: false, unique: true
        value nullable: true, unique: false, blank: true
        value nullable: true, unique: false, blank: false
    }

    String key
    String value
    String type
}
