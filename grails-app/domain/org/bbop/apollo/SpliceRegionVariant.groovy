package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class SpliceRegionVariant extends SequenceVariant {

    static String cvTerm = "splice_region_variant"
    static String ontologyId = "SO:0001630"
    static String alternateCvTerm = "splice region variant"
    static String impact = ConsequenceImpactEnum.LOW.value

    static constraints = {
    }
}
