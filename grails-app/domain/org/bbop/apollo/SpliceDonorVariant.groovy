package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class SpliceDonorVariant extends IntronVariant {

    static String cvTerm = "splice_donor_variant"
    static String ontologyId = "SO:0001575"
    static String alternateCvTerm = "splice donor variant"
    static String impact = ConsequenceImpactEnum.HIGH.value

    static constraints = {
    }
}
