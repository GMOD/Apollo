package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class UpstreamGeneVariant extends IntergenicVariant {

    static String cvTerm = "upstream_gene_variant"
    static String ontologyId = "SO:0001631"
    static String alternateCvTerm = "upstream gene variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
