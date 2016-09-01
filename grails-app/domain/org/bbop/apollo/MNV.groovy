package org.bbop.apollo

class MNV extends Substitution implements Variant {

    static String cvTerm = "MNV"
    static String ontologyId = "SO:0002007"
    static String alternateCvTerm = "multiple nucleotide variant"

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