package org.bbop.apollo

import org.bbop.apollo.gwt.shared.ConsequenceImpactEnum

class TFBindingSiteVariant extends RegulatoryRegionVariant {

    static String cvTerm = "TF_binding_site_variant"
    static String ontologyId = "SO:0001782"
    static String alternateCvTerm = "TF binding site variant"
    static String impact = ConsequenceImpactEnum.MODIFIER.value

    static constraints = {
    }
}
