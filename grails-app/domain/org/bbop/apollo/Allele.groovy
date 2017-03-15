package org.bbop.apollo

class Allele {

    String bases;
    Float alleleFrequency;
    String alterationResidue;
    String provenance;
    // TODO: interpret the type of provenance (URL, PMID, etc.)

    static constraints = {
        alleleFrequency min: 0.0F, max: 1.0F, scale: 5, nullable: true
        alterationResidue nullable: true
        provenance nullable: true
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
