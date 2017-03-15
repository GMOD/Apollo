package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class DownstreamGeneVariant extends IntergenicVariant {

    static String cvTerm = "downstream_gene_variant"
    static String ontologyId = "SO:0001632"
    static String alternateCvTerm = "downstream gene variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
