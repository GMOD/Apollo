package org.bbop.apollo

import grails.converters.JSON
import htsjdk.variant.vcf.VCFFileReader
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject


import javax.servlet.http.HttpServletResponse

class VcfController {

    def preferenceService
    def permissionService
    def vcfService
    def trackService

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
