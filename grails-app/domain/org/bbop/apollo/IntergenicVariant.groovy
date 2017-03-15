package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class IntergenicVariant extends SequenceVariant {

    static String cvTerm = "intergenic_variant"
    static String ontologyId = "SO:0001628"
    static String alternateCvTerm = "intergenic variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
