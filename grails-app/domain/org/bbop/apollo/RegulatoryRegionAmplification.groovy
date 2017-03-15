package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class RegulatoryRegionAmplification extends SequenceVariant {

    static String cvTerm = "regulatory_region_amplification"
    static String ontologyId = "SO:0001891"
    static String alternateCvTerm = "regulatory region amplification"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
