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


    def beforeInterceptor = {
        if (params.action == "featuresByName"
                || params.action == "featuresByLocation"
        ) {
            response.setHeader("Access-Control-Allow-Origin", "*")
        }
    }

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
        if (!trackService.checkPermission(request, response, organismName)) return
        int removed = TrackCache.executeUpdate("delete from TrackCache tc where tc.organismName = :commonName and tc.trackName = :trackName",[commonName:organismName,trackName: trackName])
        render new JSONObject(removed: removed) as JSON
    }

    @RestApiMethod(description = "Remove track cache for an organism", path = "/track/cache/clear/<organism name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismName", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name (required) or 'ALL' if admin")
    ])
    @Transactional
    def clearOrganismCache(String organismName) {
        if (organismName.toLowerCase().equals("all") && permissionService.isAdmin()) {
            log.info "Deleting cache for all organisms"
            JSONArray jsonArray = new JSONArray()
            Organism.all.each { organism ->
                int removed = TrackCache.executeUpdate("delete from TrackCache tc where tc.organismName = :commonName ",[commonName:organism.commonName])
                JSONObject jsonObject = new JSONObject(name: organism.commonName, removed: removed) as JSONObject
                jsonArray.add(jsonObject)
            }

            render jsonArray as JSON
        } else {
            log.info "Deleting cache for ${organismName}"
            if (!trackService.checkPermission(request, response, organismName)) return
            int removed = TrackCache.executeUpdate("delete from TrackCache tc where tc.organismName = :commonName ",[commonName:organismName])
            render new JSONObject(removed: removed) as JSON
        }

    }

    @RestApiMethod(description = "List all tracks for an organism", path = "/track/list/<organism name>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismName", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name (required)")
    ])
    @Transactional
    def getTracks(String organismName) {
        if (!trackService.checkPermission(request, response, organismName)) return
        render trackService.getAllTracks(organismName) as JSON
    }


    @RestApiMethod(description = "Get track data as an JSON within but only for the selected name", path = "/track/<organism name>/<track name>/<sequence name>/<feature name>.<type>?ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name(required)")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "featureName", type = "string", paramType = RestApiParamType.QUERY, description = "If top-level feature 'id' matches, then annotate with 'selected'=1")
            , @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false).  Use cache for request if available.")
            , @RestApiParam(name = "flatten", type = "string", paramType = RestApiParamType.QUERY, description = "Brings nested top-level components to the root level.  If not provided or 'false' it will not flatten.  Default is 'gene'." )
            , @RestApiParam(name = "type", type = "json/svg", paramType = RestApiParamType.QUERY, description = ".json or .svg")
    ])
    @Transactional
    def featuresByName(String organismString, String trackName, String sequence, String featureName, String type) {
        if (!trackService.checkPermission(request, response, organismString)) return

        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        Map paramMap = new TreeMap<>()
        paramMap.put("name", featureName)
        String flatten = params.flatten != null ? params.flatten : 'gene'
        flatten = flatten == 'false' ? '' : flatten
        paramMap.put("onlySelected", true)
        if (!ignoreCache) {
            String responseString = trackService.checkCache(organismString, trackName, sequence, featureName, type, paramMap)
            if (responseString) {
                if (type == "json") {
                    render JSON.parse(responseString)  as JSON
                    return
                }
                else
                if (type == "svg") {
                    render responseString
                    return
                }
            }
        }

        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        JSONArray renderedArray
        try {
            JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, -1, -1)
            renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        } catch (FileNotFoundException fnfe) {
            log.warn(fnfe.message)
            response.status = 404
            return
        }

        JSONArray returnArray = new JSONArray()
        for (JSONObject returnObject in renderedArray) {
            // only set if true?
            returnObject.id = createLink(absolute: true, uri: "/track/${organism.commonName}/${trackName}/${sequence}/${featureName}.json")
            if (returnObject?.name == featureName) {
                returnObject.selected = true
                returnArray.add(returnObject)
            }
        }

        if(flatten){
            returnArray  = trackService.flattenArray(returnArray,flatten)
        }

        if (type == "json") {
            trackService.cacheRequest(returnArray.toString(), organismString, trackName, sequence, featureName, type, paramMap)
            render returnArray as JSON
        } else if (type == "svg") {
            String xmlString = svgService.renderSVGFromJSONArray(returnArray)
            trackService.cacheRequest(xmlString, organismString, trackName, sequence, featureName, type, paramMap)
            render xmlString
        }

    }

    private static Set<String> getNames(def name){
        Set<String> nameSet = new HashSet<>()
        if(name){
            if(name instanceof String[]){
                name.each {
                    nameSet.add(it)
                }
            }
            else
            if(name instanceof String){
                nameSet.add(name)
            }
        }
        return nameSet
    }

    @RestApiMethod(description = "Get track data as an JSON within an range", path = "/track/<organism name>/<track name>/<sequence name>:<fmin>..<fmax>.<type>?name=<name>&onlySelected=<onlySelected>&ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID(required)")
            , @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name(required)")
            , @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name(required)")
            , @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range(required)")
            , @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range (required)")
            , @RestApiParam(name = "name", type = "string / string[]", paramType = RestApiParamType.QUERY, description = "If top-level feature 'name' matches, then annotate with 'selected'=true.  Multiple names can be passed in.")
            , @RestApiParam(name = "onlySelected", type = "string", paramType = RestApiParamType.QUERY, description = "(default false).  If 'selected'!=1 one, then exclude.")
            , @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false).  Use cache for request if available.")
            , @RestApiParam(name = "flatten", type = "string", paramType = RestApiParamType.QUERY, description = "Brings nested top-level components to the root level.  If not provided or 'false' it will not flatten.  Default is 'gene'.")
            , @RestApiParam(name = "type", type = "string", paramType = RestApiParamType.QUERY, description = ".json or .svg")
    ])
    @Transactional
    def featuresByLocation(String organismString, String trackName, String sequence, Long fmin, Long fmax, String type) {
        if (!trackService.checkPermission(request, response, organismString)) return

        Set<String> nameSet = getNames(params.name ? params.name : "")
        Boolean onlySelected = params.onlySelected != null ? params.onlySelected : false
        String flatten = params.flatten != null ? params.flatten : 'gene'
        flatten = flatten == 'false' ? '' : flatten
        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        Map paramMap = new TreeMap<>()
        paramMap.put("type", type)
        if (nameSet) {
            paramMap.put("name", nameSet)
            paramMap.put("onlySelected", onlySelected)
        }
        if (!ignoreCache) {
            String responseString = trackService.checkCache(organismString, trackName, sequence, fmin, fmax, type, paramMap)
            if (responseString) {
                if (type == "json") {
                    render JSON.parse(responseString) as JSON
                    return
                } else if (type == "svg") {
                    render responseString
                    return
                }
            }
        }
        JSONArray renderedArray
        Organism organism = preferenceService.getOrganismForToken(organismString)
        SequenceDTO sequenceDTO = new SequenceDTO(
                organismCommonName: organism.commonName
                , trackName: trackName
                , sequenceName: sequence
        )
        try {
            JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
            renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO)
        } catch (FileNotFoundException fnfe) {
            log.warn(fnfe.message)
            response.status = 404
            return
        }

        if (flatten) {
            renderedArray = trackService.flattenArray(renderedArray, flatten)
        }

        JSONArray returnArray = new JSONArray()
        for (JSONObject returnObject in renderedArray) {
            // only set if true?
            if (returnObject.name) {
                returnObject.id = createLink(absolute: true, uri: "/track/${organism.commonName}/${trackName}/${sequence}/${returnObject.name}.json")
            }
            if (nameSet) {
                if (returnObject.name && nameSet.contains(returnObject?.name)) {
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

        if (type == "json") {
            trackService.cacheRequest(renderedArray.toString(), organismString, trackName, sequence, fmin, fmax, type, paramMap)
            render renderedArray as JSON
        } else if (type == "svg") {
            String xmlString = svgService.renderSVGFromJSONArray(returnArray)
            trackService.cacheRequest(xmlString, organismString, trackName, sequence, fmin, fmax, type, paramMap)
            render xmlString
        }
    }

    def biolink(String organismString, String trackName, String sequence, Long fmin, Long fmax) {
        if (!trackService.checkPermission(request, response, organismString)) return
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
        if (!trackService.checkPermission(request, response, organismString)) return
        JSONArray filteredList = trackService.getNCList(trackName, organismString, sequence, fmin, fmax)
        render filteredList as JSON
    }

}
