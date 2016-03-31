package org.bbop.apollo

class Proxy {

    static constraints = {
        fallbackOrder nullable: true
        lastSuccess nullable: true
        lastFail nullable: true
        referenceUrl url: true, nullable: false,blank: false
        targetUrl url: true, nullable: false,blank: false
        active nullable: false
    }

    static mapping= {
        active defaultValue: true
    }

    String referenceUrl
    String targetUrl
    Boolean active
    Integer fallbackOrder
    Date lastSuccess
    Date lastFail
}
