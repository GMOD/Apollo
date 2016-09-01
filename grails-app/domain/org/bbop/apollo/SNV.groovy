package org.bbop.apollo

class SNV extends Substitution implements Variant {

    static String cvTerm = "SNV"
    static String ontologyId = "SO:0001483"
    static String alternateCvTerm = "single nucleotide variant"

    String referenceBases

    // TODO: Should variantInfo use a separate class than FeatureProperty

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
