package org.bbop.apollo

import grails.gorm.transactions.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONException
import org.grails.web.json.JSONObject

@Transactional
class JsonWebUtilityService {

  def featureService
  JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
    JSONObject jsonFeatureContainer = new JSONObject()
    JSONArray jsonFeatures = new JSONArray()
    jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures)
    for (JSONObject feature : features) {
      jsonFeatures.put(feature)
    }
    return jsonFeatureContainer
  }

  JSONObject createJSONFeatureContainerFromFeatures(Feature... features) throws JSONException {
    def jsonObjects = new ArrayList()
    for (Feature feature in features) {
      JSONObject featureObject = featureService.convertFeatureToJSON(feature, false)
      jsonObjects.add(featureObject)
    }
    return createJSONFeatureContainer(jsonObjects as JSONObject[])
  }
}
