package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONObject

@Deprecated
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
        CV cv = CV.findOrSaveByName(jsonCVTerm.getJSONObject(FeatureStringEnum.CV.value).getString(FeatureStringEnum.NAME.value))
        CVTerm cvTerm = CVTerm.findOrSaveByNameAndCv(jsonCVTerm.getString(FeatureStringEnum.NAME.value),cv)
        return cvTerm
    }

    JSONObject convertCVTermToJSON(CVTerm cvTerm){
        JSONObject jsonCVTerm = new JSONObject();
        JSONObject jsonCV = new JSONObject();
        jsonCVTerm.put(FeatureStringEnum.CV.value, jsonCV);
        jsonCV.put(FeatureStringEnum.NAME.value, cvTerm.getCv().getName());
        jsonCVTerm.put(FeatureStringEnum.NAME.value, cvTerm.getName());
        return jsonCVTerm;
    }

    /**
     * TODO: replace with a proper subclass
     * @return
     */
    Collection<CVTerm> getFrameshifts() {
        List<CVTerm> cvTermList = new ArrayList<>()
        cvTermList.add(getTerm(FeatureStringEnum.MINUS1FRAMESHIFT))
        cvTermList.add(getTerm(FeatureStringEnum.MINUS2FRAMESHIFT))
        cvTermList.add(getTerm(FeatureStringEnum.PLUS1FRAMESHIFT))
        cvTermList.add(getTerm(FeatureStringEnum.PLUS2FRAMESHIFT))
        return cvTermList
    }
}
