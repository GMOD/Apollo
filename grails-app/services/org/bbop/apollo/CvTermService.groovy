package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class CvTermService {

    CVTerm getPartOf(){
        CVTerm partOfCvTerm = CVTerm.findByName(CvTermStringEnum.PART_OF.value)
    }

    CVTerm getTerm(String term){

    }


    def serviceMethod() {

    }
}
