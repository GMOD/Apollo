package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class ProteinAlteringVariant extends CodingSequenceVariant {

    static String cvTerm = "protein_altering_variant"
    static String ontologyId = "SO:0001818"
    static String alternateCvTerm = "protein altering variant"
    static String impact = ConsequenceImpactEnum.MODERATE.value

    static constraints = {
    }
}
