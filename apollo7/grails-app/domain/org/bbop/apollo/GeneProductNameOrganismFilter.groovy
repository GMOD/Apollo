package org.bbop.apollo

import org.bbop.apollo.geneProduct.GeneProduct

class GeneProductNameOrganismFilter extends OrganismFilter {

    GeneProductName geneProductName

    static constraints = {
        geneProductName nullable: false
    }
}
