package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class NMDTranscriptVariant extends SequenceVariant {

    static String cvTerm = "NMD_transcript_variant"
    static String ontologyId = "SO:0001621"
    static String alternateCvTerm = "NMD transcript variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
