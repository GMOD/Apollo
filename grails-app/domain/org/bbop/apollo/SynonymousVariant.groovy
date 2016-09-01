package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class SynonymousVariant extends CodingSequenceVariant {

    static String cvTerm = "synonymous_variant"
    static String ontologyId = "SO:0001819"
    static String alternateCvTerm = "synonymous variant"
    static String impact = ConsequenceImpactEnum.LOW.value

    static constraints = {
    }
}
