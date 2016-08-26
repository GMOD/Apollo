package org.bbop.apollo

class VariantEffect {

    int cdnaPosition;
    int cdsPosition;
    int proteinPosition;
    String referenceCodon;
    String alternateCodon;
    String referenceResidue;
    String alternateResidue;

    static constraints = {

    }

    static belongsTo = [
            feature: Feature,
            alternateAllele: Allele
    ]

    static hasMany = [
            effects: SequenceVariant
    ]
}
