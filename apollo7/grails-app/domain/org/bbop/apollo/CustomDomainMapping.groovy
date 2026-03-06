package org.bbop.apollo

class CustomDomainMapping {

    String cvTerm
    String alternateCvTerm
    String ontologyId
    Boolean isTranscript

    static constraints = {
        cvTerm nullable: false, blank: false
        alternateCvTerm nullable: true
        ontologyId nullable: false, blank: false
    }
}
