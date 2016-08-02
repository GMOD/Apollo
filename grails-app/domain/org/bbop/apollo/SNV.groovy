package org.bbop.apollo

class SNV extends Substitution {

    static constraints = {
        minorAlleleFrequency min: 0.0F, max: 1.0F, scale: 3, nullable: true
    }

    String referenceNucleotide
    String alternateNucleotide
    Float minorAlleleFrequency
    static String cvTerm = "SNV"
    static String ontologyId = "SO:0001483"
    static String alternateCvTerm = "single nucleotide variant"
}
