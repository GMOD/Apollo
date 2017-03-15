package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class NonCodingTranscriptExonVariant extends NonCodingTranscriptVariant {

    static String cvTerm = "non_coding_transcript_exon_variant"
    static String ontologyId = "SO:0001792"
    static String alternateCvTerm = "non coding transcript exon variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
