package org.bbop.apollo

import grails.converters.JSON
import htsjdk.variant.vcf.VCFFileReader
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import javax.servlet.http.HttpServletResponse

@RestApi(name = "VCF Services", description = "Methods for retrieving VCF track data as JSON")
class VcfController {

    def preferenceService
    def permissionService
    def vcfService
    def trackService

    @RestApiMethod(description = "Get VCF track data for a given range as JSON", path = "/vcf/<organism_name>/<track_name>/<sequence_name>:<fmin>..<fmax>.<type>?includeGenotypes=<includeGenotypes>&ignoreCache=<ignoreCache>", verb = RestApiVerb.GET)
    @RestApiParams(params = [
            @RestApiParam(name = "organismString", type = "string", paramType = RestApiParamType.QUERY, description = "Organism common name or ID (required)"),
            @RestApiParam(name = "trackName", type = "string", paramType = RestApiParamType.QUERY, description = "Track name by label in trackList.json (required)"),
            @RestApiParam(name = "sequence", type = "string", paramType = RestApiParamType.QUERY, description = "Sequence name (required)"),
            @RestApiParam(name = "fmin", type = "integer", paramType = RestApiParamType.QUERY, description = "Minimum range (required)"),
            @RestApiParam(name = "fmax", type = "integer", paramType = RestApiParamType.QUERY, description = "Maximum range (required)"),
            @RestApiParam(name = "type", type = "string", paramType = RestApiParamType.QUERY, description = ".json (required)"),
            @RestApiParam(name = "includeGenotypes", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default: false).  If true, will include genotypes associated with variants from VCF."),
            @RestApiParam(name = "ignoreCache", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default: false).  Use cache for request, if available."),
    ])
    def featuresByLocation(String organismString, String trackName, String sequence, Long fmin, Long fmax, String type, boolean includeGenotypes) {
        if(!trackService.checkPermission(request, response, organismString)) return

        JSONArray featuresArray = new JSONArray()
        Organism organism = preferenceService.getOrganismForToken(organismString)
        JSONObject trackListObject = trackService.getTrackList(organism.directory)
        String trackUrlTemplate
        for(JSONObject track : trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)) {
            if(track.getString(FeatureStringEnum.LABEL.value) == trackName) {
                trackUrlTemplate = track.urlTemplate
                break
            }
        }

        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        if (!ignoreCache) {
            String responseString = trackService.checkCache(organismString, trackName, sequence, fmin, fmax, type, null)
            if (responseString) {
                render JSON.parse(responseString) as JSON
                return
            }
        }

        File file = new File(organism.directory + File.separator + trackUrlTemplate)
        try {
            VCFFileReader vcfFileReader = new VCFFileReader(file)
            featuresArray = vcfService.processVcfRecords(organism, vcfFileReader, sequence, fmin, fmax, includeGenotypes)
        }
        catch (IOException e) {
            log.error(e.stackTrace)
        }

        trackService.cacheRequest(featuresArray.toString(), organismString, trackName, sequence, fmin, fmax, type, null)
        render featuresArray as JSON
    }

}
