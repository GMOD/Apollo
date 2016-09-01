package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class FrameshiftVariant extends ProteinAlteringVariant {

    static String cvTerm = "frameshift_variant"
    static String ontologyId = "SO:0001589"
    static String alternateCvTerm = "frameshift variant"
    static String impact = ConsequenceImpactEnum.HIGH.value

    static constraints = {
    }
}
