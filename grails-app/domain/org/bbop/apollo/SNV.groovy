package org.bbop.apollo

import java.util.ArrayList;

class SNV extends Substitution {

    String referenceBases
    static String cvTerm = "SNV"
    static String ontologyId = "SO:0001483"
    static String alternateCvTerm = "single nucleotide variant"

    // TODO: Should variantInfo use a separate class than FeatureProperty

    static hasMany = [
            alternateAlleles: Allele,
            variantInfo: FeatureProperty,
            variantCalls: Call
    ]
}
