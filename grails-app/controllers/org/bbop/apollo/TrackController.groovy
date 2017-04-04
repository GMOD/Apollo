package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.transaction.Transactional
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

@RestApi(name = "Track Services", description = "Methods for managing tracks")
@Transactional(readOnly = true)
class TrackController {

    // organism / track / scaffold / min / max
    def data(String trackName,String organism,String scaffold,Long fmin,Long fmax) {
        println "trackName ${trackName}"
        println "organism ${organism}"
        println "scaffold ${scaffold}"
        println "fmin ${fmin}"
        println "fmax ${fmax}"

        JSONObject jsonObject = new JSONObject()
        jsonObject.put("some","data")

        render jsonObject as JSON
    }
}
