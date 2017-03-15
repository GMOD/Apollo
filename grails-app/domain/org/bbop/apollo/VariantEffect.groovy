package org.bbop.apollo

class VariantEffect {

    Integer cdnaPosition;
    Integer cdsPosition;
    Integer proteinPosition;
    String referenceCodon;
    String alternateCodon;
    String referenceResidue;
    String alternateResidue;
    String metadata;

    static constraints = {
        cdnaPosition nullable: true
        cdsPosition nullable: true
        proteinPosition nullable: true
        referenceCodon nullable: true
        alternateCodon nullable: true
        referenceResidue nullable: true
        alternateResidue nullable: true
        metadata nullable: true
    }

    static mapping = {
        metadata type: "text"
    }

    static belongsTo = [
            feature: Feature,
            variant: SequenceAlteration,
            alternateAllele: Allele
    ]

    static hasMany = [
            effects: SequenceVariant
    ]
}
