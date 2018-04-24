package org.bbop.apollo

class Allele {

    String bases
    SequenceAlteration variant
    boolean isReference

    static constraints = {
        bases nullable: false
        variant nullable: true
    }

    static belongsTo = [
            SequenceAlteration
    ]

    static hasMany = [
            alleleInfo: AlleleInfo
    ]

    static mapping = {
        alleleInfo cascade: 'all-delete-orphan'
    }

}
