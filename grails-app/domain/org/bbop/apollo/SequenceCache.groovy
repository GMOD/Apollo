package org.bbop.apollo

class SequenceCache {

    static constraints = {
        key nullable: false, blank: false,minSize: 5
        value nullable: false
    }

    String key
    String value
    Date lastUpdated
    Date dateCreated

    static mapping = {
        key type: 'text'
        value type: 'text'
    }


}
