package org.bbop.apollo

/**
 * Transcript type meant to be extensible
 *
 */
class CustomTranscript extends Transcript implements ConfigurableFeature{

    String metaData // JSON MetaData .. or whaever

    String customCvTerm
    String customAlternateCvTerm
    String customOntologyId
    String customClassName

    static constraints = {
        metaData nullable: true, blank: true
        customCvTerm nullable: false
        customAlternateCvTerm nullable: true
        customOntologyId nullable: false
        customClassName nullable: false
    }

    static mapping = {
        metaData type: "text"
    }

    @Override
    String getCvTerm() {
        return customCvTerm
    }

    @Override
    String getOntologyId() {
        return customOntologyId
    }

    @Override
    String getAlternateCvTerm() {
        return customAlternateCvTerm
    }

    @Override
    String getClassName() {
        return customClassName
    }

    @Override
    void setClassName(String className) {
        customClassName = className
    }

    @Override
    void setCvTerm(String cvTerm) {
        customCvTerm = cvTerm
    }

    @Override
    void setAlternateCvTerm(String alternateCvTerm) {
        customAlternateCvTerm = alternateCvTerm
    }

    @Override
    void setOntologyId(String ontologyId) {
        customOntologyId = ontologyId
    }
}
