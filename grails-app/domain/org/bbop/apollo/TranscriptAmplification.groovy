package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class TranscriptAmplification extends SequenceVariant {

    static String cvTerm = "transcript_amplification"
    static String ontologyId = "SO:0001889"
    static String alternateCvTerm = "transcript amplification"
    static String impact = ConsequenceImpactEnum.HIGH.value

    static constraints = {
    }
}
