package org.bbop.apollo

class Indel extends SequenceAlteration implements Variant {

    static String cvTerm = "indel"
    static String ontologyId = "SO:1000032"
    static String alternateCvTerm = "indel"

    String referenceBases

    static hasMany = [
            alternateAlleles: Allele,
            variantInfo     : FeatureProperty,
            variantCalls    : Call
    ]

//    static mappedBy = [
//            alternateAlleles: "variant"
//    ]

    static mapping = {
        alternateAlleles cascade: 'all-delete-orphan'
        variantInfo cascade: 'all-delete-orphan'
        variantCalls cascade: 'all-delete-orphan'
    }
}
