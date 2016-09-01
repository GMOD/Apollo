package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class TFBSAblation extends RegulatoryRegionAblation {

    static String cvTerm = "TFBS_ablation"
    static String ontologyId = "SO:0001895"
    static String alternateCvTerm = "TFBS ablation"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
