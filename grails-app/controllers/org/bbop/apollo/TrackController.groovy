package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import javax.servlet.http.HttpServletResponse

@RestApi(name = "Track Services", description = "Methods for retrieving track data")
@Transactional(readOnly = true)
class TrackController {

    def preferenceService
    def permissionService
    def trackService

    /**
     * Just a convenience method
     * @param trackName
     * @param organismString
     * @param sequence
     * @return
     */
    def trackData(String organismString, String trackName, String sequence) {
        JSONObject jsonObject = trackService.getTrackData(trackName, organismString, sequence)
        render jsonObject as JSON
    }

    @RestApiMethod(description = "Get track data as an JSON within an range", path = "/track/json/<organism string>/<track name>/<sequence name>:<fmin>..<fmax>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name")
            , @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range")
            , @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range")
    ])
    def json(String organismString, String trackName, String sequence, Long fmin, Long fmax) {
        if (!checkPermission(organismString)) return
        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        JSONArray renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        render renderedArray as JSON
    }

    def biolink(String organismString, String trackName, String sequence, Long fmin, Long fmax) {
        if (!checkPermission(organismString)) return
        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
        JSONObject renderdObject = trackService.getNCListAsBioLink(filteredList)
        render renderdObject as JSON
    }

    /**
     *
     * @param trackName
     * @param organism
     * @param sequence
     * @param fmin
     * @param fmax
     * @return
     */
    // / track < organism ID or name > / <track name > /  < sequence name > / min / max
    @RestApiMethod(description = "Get track data as an NCList JSON Array within an range", path = "/track/nclist/<organism string>/<track name>/<sequence name>:<fmin>..<fmax>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name")
            , @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range")
            , @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range")
    ])
    def nclist(String organismString, String trackName, String sequence, Long fmin, Long fmax) {
        if (!checkPermission(organismString)) return
        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
        render filteredList as JSON
    }

    def checkPermission(String organismString) {
        Organism organism = preferenceService.getOrganismForToken(organismString)
        if (organism.publicMode || permissionService.checkPermissions(PermissionEnum.READ)) {
            return true
        } else {
            // not accessible to the public
            response.status = HttpServletResponse.SC_FORBIDDEN
            render ""
            return false
        }

    }
}
