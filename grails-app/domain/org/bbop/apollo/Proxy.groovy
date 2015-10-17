package org.bbop.apollo

class Proxy {

    static constraints = {
        order nullable: true
        lastSuccess nullable: true
        lastFail nullable: true
    }

    static mapping= {
        active defaultValue: true
    }

    String referenceUrl
    String targetUrl
    Boolean active
    Integer order
    Date lastSuccess
    Date lastFail
}
