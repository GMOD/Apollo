package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class StopRetainedVariant extends SynonymousVariant {

    static String cvTerm = "stop_retained_variant"
    static String ontologyId = "SO:0001567"
    static String alternateCvTerm = "stop retained variant"
    static String impact = ConsequenceImpactEnum.LOW.value

    static constraints = {
    }
}
