package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class IntronVariant extends SequenceVariant {

    static String cvTerm = "intron_variant"
    static String ontologyId = "SO:0001627"
    static String alternateCvTerm = "intron variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
