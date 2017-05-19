package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi

@RestApi(name = "Track Services", description = "Methods for managing tracks")
@Transactional(readOnly = true)
class TrackController {

    def preferenceService
    def trackService
    def trackMapperService

    /**
     * Just a convenience method
     * @param trackName
     * @param organismString
     * @param scaffold
     * @return
     */
    def trackData(String organismString, String trackName, String scaffold) {
        JSONObject jsonObject = trackService.getTrackData(trackName, organismString, scaffold)
        render jsonObject as JSON
    }

    def json(String organismString, String trackName, String scaffold, Long fmin, Long fmax) {
        JSONArray filteredList = trackService.getNCList(trackName, organismString, scaffold, fmin, fmax)
        JSONArray clasesForTrack = trackService.getClassesForTrack(trackName, organismString, scaffold)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: scaffold
        )
        trackMapperService.storeTrack(sequenceDTO,clasesForTrack)
        JSONArray renderedArray = trackService.convertAllNCListToObject(filteredList,sequenceDTO)
        render renderedArray as JSON
    }

    def biolink(String organismString, String trackName, String scaffold, Long fmin, Long fmax) {
        JSONArray filteredList = trackService.getNCList(trackName, organismString, scaffold, fmin, fmax)
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
    def nclist(String organismString, String trackName, String scaffold, Long fmin, Long fmax) {
        JSONArray filteredList = trackService.getNCList(trackName, organismString, scaffold, fmin, fmax)
        if(!filteredList){
            response.status = 404
            render ""
            return
        }
        render filteredList as JSON
    }

}
