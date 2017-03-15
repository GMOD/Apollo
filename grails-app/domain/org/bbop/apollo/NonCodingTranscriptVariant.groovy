package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class NonCodingTranscriptVariant extends SequenceVariant {

    static String cvTerm = "non_coding_transcript_variant"
    static String ontologyId = "SO:0001619"
    static String alternateCvTerm = "non coding transcript variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
