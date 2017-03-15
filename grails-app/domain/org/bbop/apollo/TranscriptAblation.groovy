package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class TranscriptAblation extends SequenceVariant {

    static String cvTerm = "transcript_ablation"
    static String ontologyId = "SO:0001893"
    static String alternateCvTerm = "transcript ablation"
    static String impact = ConsequenceImpactEnum.HIGH.value

    static constraints = {
    }
}
