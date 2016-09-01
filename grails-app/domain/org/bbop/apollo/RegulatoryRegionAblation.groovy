package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class RegulatoryRegionAblation extends SequenceVariant {

    static String cvTerm = "regulatory_region_ablation"
    static String ontologyId = "SO:0001894"
    static String alternateCvTerm = "regulatory region ablation"
    static String impact = ConsequenceImpactEnum.MODERATE.value

    static constraints = {
    }
}
