package org.bbop.apollo

class SNV extends Substitution {

    static constraints = {
    }

    String referenceNucleotide
    String alternateNucleotide
    static String cvTerm = "SNV"
    static String ontologyId = "SO:0001483"
    static String alternateCvTerm = "single nucleotide variant"
}
