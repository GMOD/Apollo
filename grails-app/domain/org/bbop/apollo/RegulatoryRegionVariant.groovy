package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class RegulatoryRegionVariant extends SequenceVariant {

    static String cvTerm = "regulatory_region_variant"
    static String ontologyId = "SO:0001566"
    static String alternateCvTerm = "regulatory region variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
