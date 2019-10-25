package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.plugins.metrics.groovy.Timed

@Transactional
class JsonWebService {

  @NotTransactional
  @Timed
  JSONObject createJSONFeatureContainer(JSONObject... features) throws JSONException {
    JSONObject jsonFeatureContainer = new JSONObject()
    JSONArray jsonFeatures = new JSONArray()
    jsonFeatureContainer.put(FeatureStringEnum.FEATURES.value, jsonFeatures)
    for (JSONObject feature : features) {
      jsonFeatures.put(feature)
    }
    return jsonFeatureContainer
  }
}
