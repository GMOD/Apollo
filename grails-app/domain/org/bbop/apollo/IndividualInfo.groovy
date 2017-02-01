package org.bbop.apollo

class IndividualInfo {

    String tag
    String value

    static constraints = {
    }

    static belongsTo = [
            individual: Individual
    ]
}
