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

        File file = new File(organism.directory + File.separator + trackUrlTemplate)
        try {
            VCFFileReader vcfFileReader = new VCFFileReader(file)
            featuresArray = vcfService.processVcfRecords(organism, vcfFileReader, sequence, fmin, fmax, includeGenotypes)
        }
        catch (IOException e) {
            log.error(e.stackTrace)
        }

        // TODO
//        if(type == "json") {
//            render featuresArray as JSON
//        }
//        else if(type == "svg") {
//            String xmlString = svgService.renderSVGFromJSONArray(featuresArray)
//            render xmlString
//        }

        render featuresArray as JSON

    }

}
