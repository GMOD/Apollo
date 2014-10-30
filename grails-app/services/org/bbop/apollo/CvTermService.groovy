package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject

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

    CVTerm convertJSONToCVTerm(JSONObject jsonCVTerm){
        CV cv = CV.findOrSaveByName(jsonCVTerm.getJSONObject(FeatureStringEnum.CV).getString(FeatureStringEnum.NAME))
        CVTerm cvTerm = CVTerm.findOrSaveByNameAndCv(jsonCVTerm.getString(FeatureStringEnum.NAME),cv)
        return cvTerm
    }
}
