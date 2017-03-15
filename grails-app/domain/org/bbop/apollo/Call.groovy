package org.bbop.apollo

class Call {

    String genotype
    Boolean phased

    static constraints = {
    }

    static belongsTo = [
            callSet: CallSet,
            variant: SequenceAlteration
    ]
}
