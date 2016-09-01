package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class CodingSequenceVariant extends CodingTranscriptVariant {

    static String cvTerm = "coding_sequence_variant"
    static String ontologyId = "SO:0001580"
    static String alternateCvTerm = "coding sequence variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
