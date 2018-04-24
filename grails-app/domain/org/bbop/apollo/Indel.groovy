package org.bbop.apollo

class Indel extends SequenceAlteration {

    static constraints = {
    }

    static String cvTerm = "indel"
    static String ontologyId = "SO:0001059"
    static String alternateCvTerm = "indel"
    Allele referenceAllele

    static hasMany = [
            alternateAlleles: Allele
    ]

}
