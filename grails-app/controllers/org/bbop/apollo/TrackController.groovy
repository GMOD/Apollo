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
    def grailsApplication
    def svgService

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

    @RestApiMethod(description = "Remove track cache for an organism and track", path = "/track/cache/clear/<organism name>/<track name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismName", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name (required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name (required)")
    ])
    @Transactional
    def clearTrackCache(String organismName, String trackName) {
        if (!checkPermission(organismName)) return
        int removed = TrackCache.countByOrganismNameAndTrackName(organismName, trackName)
        TrackCache.deleteAll(TrackCache.findAllByOrganismNameAndTrackName(organismName, trackName))
        render new JSONObject(removed: removed) as JSON
    }

    @RestApiMethod(description = "Remove track cache for an organism", path = "/track/cache/clear/<organism name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismName", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name (required)")
    ])
    @Transactional
    def clearOrganismCache(String organismName) {
        if (!checkPermission(organismName)) return
        int removed = TrackCache.countByOrganismName(organismName)
        TrackCache.deleteAll(TrackCache.findAllByOrganismName(organismName))
        render new JSONObject(removed: removed) as JSON
    }


    @RestApiMethod(description = "Get track data as an JSON within but only for the selected name", path = "/track/<organism name>/<track name>/<sequence name>/<feature name>.<type>?ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name(required)")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "featureName", type = "string", paramType = RestApiParamType.QUERY, description = "If top-level feature 'id' matches, then annotate with 'selected'=1")
            , @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false).  Use cache for request if available.")
            , @RestApiParam(name = "type", type = "json/svg", paramType = RestApiParamType.QUERY, description = ".json or .svg")
    ])
    @Transactional
    def featuresByName(String organismString, String trackName, String sequence, String featureName, String type) {
        if (!checkPermission(organismString)) return

        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        Map paramMap = new TreeMap<>()
        paramMap.put("name", featureName)
        paramMap.put("onlySelected", true)
        if (!ignoreCache) {
            JSONArray responseArray = trackService.checkCache(organismString, trackName, sequence, featureName, paramMap)
            if (responseArray != null) {
                render responseArray as JSON
                return
            }
        }

        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, -1, -1)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        JSONArray renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        JSONArray returnArray = new JSONArray()

        for (returnObject in renderedArray) {
            // only set if true?
            returnObject.id = createLink(absolute: true, uri: "/track/${organism.commonName}/${trackName}/${sequence}/${featureName}.json")
            if (returnObject?.name == featureName) {
                returnObject.selected = true
                returnArray.add(returnObject)
            }
        }

        trackService.cacheRequest(returnArray, organismString, trackName, sequence, featureName, paramMap)

        if (type == "json") {
            render returnArray as JSON
        } else if (type == "svg") {
            render svgService.renderSVGFromJSONArray(returnArray)
        }

    }


    @RestApiMethod(description = "Get track data as an JSON within an range", path = "/track/<organism name>/<track name>/<sequence name>:<fmin>..<fmax>.<type>?name=<name>&onlySelected=<onlySelected>&ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name(required)")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range(required)")
            , @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range (required)")
            , @RestApiParam(name = "name", type = "string", paramType = RestApiParamType.QUERY, description = "If top-level feature 'id' matches, then annotate with 'selected'=1")
            , @RestApiParam(name = "onlySelected", type = "string", paramType = RestApiParamType.QUERY, description = "(default false).  If 'selected'!=1 one, then exclude.")
            , @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false).  Use cache for request if available.")
            , @RestApiParam(name = "type", type = "json/svg", paramType = RestApiParamType.QUERY, description = ".json or .svg")
    ])
    @Transactional
    def featuresByLocation(String organismString, String trackName, String sequence, Long fmin, Long fmax, String type) {
        if (!checkPermission(organismString)) return

        String name = params.name ? params.name : ""
        Boolean onlySelected = params.onlySelected != null ? params.onlySelected : false
        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        Map paramMap = new TreeMap<>()
        if (name) {
            paramMap.put("name", name)
            paramMap.put("onlySelected", onlySelected)
        }
        if (!ignoreCache) {
            JSONArray responseArray = trackService.checkCache(organismString, trackName, sequence, fmin, fmax, paramMap)
            if (responseArray != null) {
                render responseArray as JSON
                return
            }
        }
        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        JSONArray renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        JSONArray returnArray = new JSONArray()

        for (returnObject in renderedArray) {
            // only set if true?
            if (returnObject.name) {
                returnObject.id = createLink(absolute: true, uri: "/track/${organism.commonName}/${trackName}/${sequence}/${returnObject.name}.json")
            }
            if (name) {
                if (returnObject?.name == name) {
                    returnObject.selected = true
                    if (onlySelected) {
                        returnArray.add(returnObject)
                    }
                }
            }
        }

        if (onlySelected) {
            renderedArray = returnArray
        }

        trackService.cacheRequest(renderedArray, organismString, trackName, sequence, fmin, fmax, paramMap)

        if (type == "json") {
            render renderedArray as JSON
        } else if (type == "svg") {
            render svgService.renderSVGFromJSONArray(renderedArray)
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
