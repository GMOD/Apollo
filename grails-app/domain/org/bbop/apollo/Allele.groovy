package org.bbop.apollo

class Allele {

    String bases;
    Float alleleFrequency;

    static constraints = {
        alleleFrequency min: 0.0F, max: 1.0F, scale: 3, nullable: true
    }

    static belongsTo = [
            variant: SequenceAlteration
    ]

    // TODO: Should alleleInfo use a separate class than FeatureProperty

    static hasMany = [
            alleleInfo: FeatureProperty,
            variantEffects: VariantEffect
    ]

}
