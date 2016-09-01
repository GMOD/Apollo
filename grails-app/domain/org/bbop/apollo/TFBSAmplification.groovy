package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class TFBSAmplification extends RegulatoryRegionAmplification {

    static String cvTerm = "TFBS_amplification"
    static String ontologyId = "SO:0001892"
    static String alternateCvTerm = "TFBS amplification"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
