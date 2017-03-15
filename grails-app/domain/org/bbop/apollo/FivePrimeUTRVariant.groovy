package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class FivePrimeUTRVariant extends CodingTranscriptVariant {

    static String cvTerm = "5_prime_UTR_variant"
    static String ontologyId = "SO:0001623"
    static String alternateCvTerm = "5 prime UTR variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
