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

    @RestApiMethod(description = "Get track data as an JSON within but only for the selected name", path = "/track/<organism string>/<track name>/<sequence name>/<feature name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name(required)")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "featureName", type = "string", paramType = RestApiParamType.QUERY, description = "If top-level feature 'id' matches, then annotate with 'selected'=1")
    ])
    def jsonName(String organismString, String trackName, String sequence, String featureName) {
        if (!checkPermission(organismString)) return
        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, -1, -1)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        JSONArray renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        if (featureName) {
            JSONArray returnArray = new JSONArray()

            for (returnObject in renderedArray) {
                // only set if true?
                if (returnObject?.id == featureName) {
                    returnObject.selected = true
                    returnArray.add(returnObject)
                }
            }

            render returnArray as JSON
        } else {
            render renderedArray as JSON
        }
    }

    @RestApiMethod(description = "Get track data as an JSON within an range", path = "/track/<organism string>/<track name>/<sequence name>:<fmin>..<fmax>?filter=<filter>&excludeFilter=<excludeFilter>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name(required)")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range(required)")
            , @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range (required)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "If top-level feature 'id' matches, then annotate with 'selected'=1")
            , @RestApiParam(name = "onlySelected", type = "string", paramType = RestApiParamType.QUERY, description = "(default false).  If 'selected'!=1 one, then exclude.")
    ])
    def json(String organismString, String trackName, String sequence, Long fmin, Long fmax) {
        if (!checkPermission(organismString)) return

        String name = params.name ?: ""
        Boolean onlySelected = params.onlySelected ?: false

        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        JSONArray renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        if (name) {
            JSONArray returnArray = new JSONArray()

            for (returnObject in renderedArray) {
                // only set if true?
                if (returnObject?.id == name) {
                    returnObject.selected = true
                    returnArray.add(returnObject)
                } else if (!onlySelected) {
                    returnArray.add(returnObject)
                }
            }

            render returnArray as JSON
        } else {
            render renderedArray as JSON
        }
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
    // TODO: this is just for debuggin
    // track < organism ID or name > / <track name > /  < sequence name > / min / max
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
