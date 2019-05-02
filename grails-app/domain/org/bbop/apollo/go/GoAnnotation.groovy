package org.bbop.apollo.go

import org.bbop.apollo.Feature
import org.bbop.apollo.gwt.shared.go.EvidenceCode
import org.bbop.apollo.gwt.shared.go.Qualifier


class GoAnnotation {


    Feature feature
    GoTerm goTerm
    EvidenceCode evidenceCode

    static hasMany = [
            qualifiers: Qualifier
            ,withOrFroms: WithOrFrom
            ,references: Reference
    ]

    String getWithOrFromString(){
        StringBuilder withOrFromStringBuilder = new StringBuilder()
        for(WithOrFrom withOrFrom : withOrFroms){
            withOrFromStringBuilder.append(withOrFrom.getDisplay())
            withOrFromStringBuilder.append(" ")
        }
        return withOrFromStringBuilder.toString()
    }

    String getReferenceString(){
        StringBuilder referenceStringBuilder = new StringBuilder()
        for(Reference reference: references){
            referenceStringBuilder.append(reference.getReferenceString())
            referenceStringBuilder.append(" ")
        }
        return referenceStringBuilder.toString()
    }
}
