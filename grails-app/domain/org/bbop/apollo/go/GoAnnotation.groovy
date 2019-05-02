package org.bbop.apollo.go


class GoAnnotation {


    GoTerm goTerm
    EvidenceCode evidenceCode
    List<Qualifier> qualifierList
    List<WithOrFrom> withOrFromList
    List<Reference> referenceList

    void addWithOrFrom(WithOrFrom withOrFrom) {
        if(withOrFromList==null){
            withOrFromList = new ArrayList<>()
        }
        withOrFromList.add(withOrFrom)
    }

    void addReference(Reference reference) {
        if(referenceList==null){
            referenceList = new ArrayList<>()
        }
        referenceList.add(reference)
    }

    String getWithOrFromString(){
        StringBuilder withOrFromStringBuilder = new StringBuilder()
        for(WithOrFrom withOrFrom : getWithOrFromList()){
            withOrFromStringBuilder.append(withOrFrom.getDisplay())
            withOrFromStringBuilder.append(" ")
        }
        return withOrFromStringBuilder.toString()
    }

    String getReferenceString(){
        StringBuilder referenceStringBuilder = new StringBuilder()
        for(Reference reference: getReferenceList()){
            referenceStringBuilder.append(reference.getReferenceString())
            referenceStringBuilder.append(" ")
        }
        return referenceStringBuilder.toString()
    }
}
