package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class CvTermService {

    CVTerm getPartOf(){
        CVTerm.findByName(CvTermStringEnum.PART_OF.value)
    }

    CVTerm getTerm(String term){
        CVTerm.findByName(term)
    }

    CVTerm getTerm(FeatureStringEnum featureStringEnum){
        CVTerm.findByName(featureStringEnum.value)
    }

    CVTerm getTranscript(){
        CVTerm.findByName(FeatureStringEnum.TRANSCRIPT.value)
    }
}
