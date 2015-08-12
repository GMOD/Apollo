package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.projection.ReferenceTrack
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * The goal of this track is to create a ReferenceTrack and
 * very quickly
 */
@Transactional(readOnly = true)
class ProjectionService {


    def createReferenceTrack(JSONObject inputObject) {
        ReferenceTrack referenceTrack = new ReferenceTrack()

        JSONArray features = inputObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        Integer buffer = 0
        Integer paddingConstant = 20
        for(int i = 0 ; i < features.size() ;i++){
            JSONObject feature = features.getJSONObject(i)
            JSONObject featureLocation = feature.getJSONObject(FeatureStringEnum.LOCATION.value);
            Integer fmin = featureLocation.getInt(FeatureStringEnum.FMIN.value)
            Integer fmax = featureLocation.getInt(FeatureStringEnum.FMAX.value)
            
        }




    }
}
