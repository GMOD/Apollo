package org.bbop.apollo.go

import org.bbop.apollo.Feature
import org.bbop.apollo.User


class GoAnnotation {

    static constraints = {
        feature nullable: false
        goRef nullable: false, blank: false
        evidenceRef nullable: false, blank: false
        geneProductRelationshipRef nullable: true, blank: false
        negate nullable: false
        withOrFromArray nullable: true, blank: true
        referenceArray nullable: true, blank: true
        dateCreated nullable: false
        lastUpdated nullable: false
    }

    static hasMany = [
            owners: User
    ]

    Feature feature
    String goRef
    String evidenceRef
    String geneProductRelationshipRef
    Boolean negate
    String withOrFromArray
    String referenceArray
    Date lastUpdated
    Date dateCreated

}
