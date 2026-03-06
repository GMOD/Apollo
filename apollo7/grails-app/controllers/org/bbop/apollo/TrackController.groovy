package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.sequence.SequenceDTO
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject


@Transactional(readOnly = true)
class TrackController {

    def preferenceService
    def permissionService
    def trackService
    def svgService

  final double DEFAULT_OVERLAP_FILTER = 50.0

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

    @Transactional
    def clearTrackCache(String organismName, String trackName) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(requestObject,PermissionEnum.READ)
        } catch (e) {
            def error = [error: e.message]
            render error as JSON
        }
        if (!trackService.checkPermission(request, response, organismName)) return
        int removed = TrackCache.executeUpdate("delete from TrackCache tc where tc.organismName = :commonName and tc.trackName = :trackName",[commonName:organismName,trackName: trackName])
        render new JSONObject(removed: removed) as JSON
    }

    @Transactional
    def clearOrganismCache(String organismName) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(requestObject,PermissionEnum.READ)
        } catch (e) {
            def error = [error: e.message]
            render error as JSON
        }
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

    @Transactional
    def getTracks(String organismName) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(requestObject,PermissionEnum.READ)
        } catch (e) {
            def error = [error: e.message]
            render error as JSON
        }
        if (!trackService.checkPermission(request, response, organismName)) return
        render trackService.getAllTracks(organismName) as JSON
    }


    @Transactional
    def featuresByName(String organismString, String trackName, String sequence, String featureName, String type) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(requestObject,PermissionEnum.READ)
        } catch (e) {
            def error = [error: e.message]
            render error as JSON
        }
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

    @Transactional
    def featuresByLocation(String organismString, String trackName, String sequence, Long fmin, Long fmax, String type) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(requestObject,PermissionEnum.READ)
        } catch (e) {
            def error = [error: e.message]
            render error as JSON
        }
        if (!trackService.checkPermission(request, response, organismString)) return

        Set<String> nameSet = getNames(params.name ? params.name : "")
        Boolean onlySelected = params.onlySelected != null ? params.onlySelected : false
        Boolean unique = params.unique != null ? params.unique : false
        String flatten = params.flatten != null ? params.flatten : 'gene'
        flatten = flatten == 'false' ? '' : flatten
        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        double overlapFilter = params.overlapFilter != null ? Double.valueOf(params.overlapFilter) : DEFAULT_OVERLAP_FILTER
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
          // there should be 2 nclists, one for 20 and one for 40
            Long filterValue = overlapFilter * (fmax - fmin)
            Long fminAdjusted = fmin - filterValue < 0 ? fmin : fmin - filterValue
            Long fmaxAdjusted = fmax + filterValue
            renderedArray = trackService.convertAllNCListToObject(filteredList, sequenceDTO,fminAdjusted,fmaxAdjusted)
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
                returnObject.sourceUrl = createLink(absolute: true, uri: "/track/${organism.commonName}/${trackName}/${sequence}/${returnObject.name}.json")
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

        if(unique){
            renderedArray = renderedArray.unique{  it.name } as JSONArray
        }
        renderedArray = renderedArray.findAll{  it ->
          return (it.fmax - it.fmin) < (overlapFilter*(fmax - fmin))
        } as JSONArray

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
