package org.bbop.apollo

class MNV extends Substitution {

    static String cvTerm  = "MNV"
    static String ontologyId = "SO:0002007"
    static String alternateCvTerm = "multiple nucleotide variant"

    static hasMany = [
            alleles: Allele,
            variantInfo: VariantInfo
    ]

    def getReferenceAllele() {
        alleles.each {
            if (it.isReference) return it
        }
    }
}
