package org.bbop.apollo

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import htsjdk.variant.vcf.VCFFileReader
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import jakarta.servlet.http.HttpServletResponse

@Transactional
class VcfController {

    def preferenceService
    def permissionService
    def vcfService
    def trackService

    def beforeInterceptor = {
        if (params.action == "featuresByLocation") {
            response.setHeader("Access-Control-Allow-Origin", "*")
        }
    }


    def featuresByLocation(String organismString, String trackName, String sequence, Long fmin, Long fmax, String type, boolean includeGenotypes) {
        JSONObject requestObject = permissionService.handleInput(request, params)
        try {
            permissionService.hasPermissions(requestObject,PermissionEnum.READ)
        } catch (e) {
            def error = [error: e.message]
            render error as JSON
            return
        }
        if(!trackService.checkPermission(request, response, organismString)) return

        JSONArray featuresArray = new JSONArray()
        Organism organism = preferenceService.getOrganismForToken(organismString)
        JSONObject trackListObject = trackService.getTrackList(organism.directory)

        Boolean ignoreCache = params.ignoreCache != null ? Boolean.valueOf(params.ignoreCache) : false
        if (!ignoreCache) {
            String responseString = trackService.checkCache(organismString, trackName, sequence, fmin, fmax, type, null)
            if (responseString) {
                render JSON.parse(responseString) as JSON
                return
            }
        }

        String trackUrlTemplate = null
        for(JSONObject track : trackListObject.getJSONArray(FeatureStringEnum.TRACKS.value)) {
            log.debug "comparing ${track.label} to ${trackName}"
            if(track.getString(FeatureStringEnum.LABEL.value) == trackName) {
                log.debug "found ${track} -> ${track.urlTemplate}"
                trackUrlTemplate = track.urlTemplate
                break
            }
        }
        if(!trackUrlTemplate){
            throw new RuntimeException("Track url template not found for '${trackName}'")
        }

        File file = new File(organism.directory + File.separator + trackUrlTemplate)
        try {
            VCFFileReader vcfFileReader = new VCFFileReader(file)
            featuresArray = vcfService.processVcfRecords(organism, vcfFileReader, sequence, fmin, fmax, includeGenotypes)
        }
        catch (IOException e) {
            log.error(e.message, e)
        }

        // returning from the cache is more consistent.
        trackService.cacheRequest(featuresArray.toString(), organismString, trackName, sequence, fmin, fmax, type, null)
        String responseString = trackService.checkCache(organismString, trackName, sequence, fmin, fmax, type, null)
        if (responseString) {
            render JSON.parse(responseString) as JSON
            return
        }
        render new JSONObject() as JSON    }

}
