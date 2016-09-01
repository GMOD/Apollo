package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class SpliceAcceptorVariant extends IntronVariant {

    static String cvTerm = "splice_acceptor_variant"
    static String ontologyId = "SO:0001574"
    static String alternateCvTerm = "splice acceptor variant"
    static String impact = ConsequenceImpactEnum.HIGH.value

    static constraints = {
    }
}
