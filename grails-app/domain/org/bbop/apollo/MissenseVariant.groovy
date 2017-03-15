package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class MissenseVariant extends ProteinAlteringVariant {

    static String cvTerm = "missense_variant"
    static String ontologyId = "SO:0001583"
    static String alternateCvTerm = "missense variant"
    static String impact = ConsequenceImpactEnum.MODERATE.value

    static constraints = {
    }
}
