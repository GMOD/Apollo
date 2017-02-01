package org.bbop.apollo

class SequenceAlteration extends SequenceFeature{

    static String cvTerm  = "SequenceAlteration"
    static String ontologyId = "SO:0001059"

    static constraints = {
        alterationResidue nullable: true
        alterationType nullable: true
        referenceBases nullable: true
        individual nullable: true
    }

    String alterationResidue
    String alterationType
    String referenceBases

    static hasMany = [
            alternateAlleles: Allele,
            variantInfo     : FeatureProperty,
            variantCalls    : Call
    ]

    static belongsTo = [
            individual: Individual
    ]

    static mapping = {
        alternateAlleles cascade: 'all-delete-orphan'
        variantInfo cascade: 'all-delete-orphan'
        variantCalls cascade: 'all-delete-orphan'
    }

    /** Get the offset added by the sequence alteration.
     *
     * @return Offset added by the sequence alteration
     */
    public int getOffset() {
        return 0;
    }

}
