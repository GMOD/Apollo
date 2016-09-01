package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class ThreePrimeUTRVariant extends CodingTranscriptVariant {

    static String cvTerm = "3_prime_UTR_variant"
    static String ontologyId = "SO:0001624"
    static String alternateCvTerm = "3 prime UTR variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
