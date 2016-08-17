package org.bbop.apollo

import java.util.ArrayList;

class MNV extends Substitution {

    static constraints = {
        minorAlleleFrequency min: 0.0F, max: 1.0F, scale: 3, nullable: true
    }

    String referenceBases
    String alternateBases
    Float minorAlleleFrequency
    static String cvTerm = "MNV"
    static String ontologyId = "SO:0002007"
    static String alternateCvTerm = "multiple nucleotide variant"
}