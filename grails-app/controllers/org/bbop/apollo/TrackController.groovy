package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi

@RestApi(name = "Track Services", description = "Methods for managing tracks")
@Transactional(readOnly = true)
class TrackController {

    def preferenceService
    def trackMapperService
    def trackService


    def json(String trackName, String organism, String scaffold, Long fmin, Long fmax) {
        JSONArray filteredList = trackService.getNCList(trackName, organism, scaffold, fmin, fmax)

        JSONObject renderdObject = trackService.getNCListAsObject(filteredList)
        render renderdObject as JSON
    }

    def biolink(String trackName, String organism, String scaffold, Long fmin, Long fmax) {
        JSONArray filteredList = trackService.getNCList(trackName, organism, scaffold, fmin, fmax)
        JSONObject renderdObject = trackService.getNCListAsBioLink(filteredList)
        render renderdObject as JSON
    }

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
    def nclist(String trackName, String organism, String scaffold, Long fmin, Long fmax) {
        JSONArray filteredList = trackService.getNCList(trackName, organism, scaffold, fmin, fmax)
        if(!filteredList){
            response.status = 404
            render ""
            return
        }
        render filteredList as JSON
    }

}
