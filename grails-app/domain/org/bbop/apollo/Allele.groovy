package org.bbop.apollo

class Allele {

    String bases;
    Float alleleFrequency;

    static constraints = {
        alleleFrequency min: 0.0F, max: 1.0F, scale: 5, nullable: true
    }

    static belongsTo = [
            variant: SequenceAlteration
    ]

    static hasMany = [
            alleleInfo: AlleleInfo,
            variantEffects: VariantEffect
    ]

    static mapping = {
        alleleInfo cascade: 'all-delete-orphan'
        variantEffects cascade: 'all-delete-orphan'
    }

}
