package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
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

    def preferenceService
    def trackMapperService
    /**
     *
     * @param trackName
     * @param organism
     * @param scaffold
     * @param fmin
     * @param fmax
     * @return
     */
    // / track < organism ID or name > / <track name > /  < scaffold name > / min / max
    def view(String trackName,String organism,String scaffold,Long fmin,Long fmax) {
        println "trackName ${trackName}"
        println "organism ${organism}"
        println "scaffold ${scaffold}"
        println "fmin ${fmin}"
        println "fmax ${fmax}"

        assert fmin < fmax

        String jbrowseDirectory = preferenceService.getOrganismForToken(organism)?.directory

        // 1. get the trackData.json file

        String trackPath = "${jbrowseDirectory}/tracks/${trackName}/${scaffold}"
        String trackDataFilePath = "${trackPath}/trackData.json"

        File file = new File(trackDataFilePath)
        if(!file.exists()){
            println "file does not exist ${trackDataFilePath}"
            response.status = 404
            render ""
            return
        }

        JSONObject trackObject = JSON.parse(file.text) as JSONObject
        JSONArray nclistArray = trackObject.getJSONObject("intervals").getJSONArray("nclist")



        // 1 - extract the appropriate region for fmin / fmax
        JSONArray filteredList = filterList(nclistArray,fmin,fmax)
        println "filtered list size ${filteredList.size()} from original ${nclistArray.size()}"


        // 2 - convert each element to JSON




        render filteredList as JSON
    }

    JSONArray filterList(JSONArray inputArray, long fmin, long fmax) {
        JSONArray jsonArray = new JSONArray()

        for(innerArray in inputArray){
            println "handling array"
            // if there is an overlap
            if(  !(innerArray[2]<fmin || innerArray[1] > fmax) ){
                // then no
                jsonArray.add(innerArray)
            }
        }


        return jsonArray
    }
}
