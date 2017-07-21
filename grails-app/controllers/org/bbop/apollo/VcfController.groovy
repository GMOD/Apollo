package org.bbop.apollo

import htsjdk.variant.vcf.VCFFileReader
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection

class VcfController {

    def vcfService
    def permissionService
    def preferenceService
    def projectionService
    def trackService

    def index() { }

    JSONObject global(String trackName, Long organismId) {
        log.info "trackName: ${trackName}"
        log.info "organismId: ${organismId}"
        JSONObject data = permissionService.handleInput(request, params)
        Organism organism = Organism.findById(organismId)
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(organism,trackName)
        JSONObject returnObject = new JSONObject()
        returnObject.put("featureDensity", 0.2);
        // TODO
        render returnObject as JSON
    }

    def region() {
        // TODO
        render new JSONObject() as JSON

    }

    def regionFeatureDensities() {
        // TODO
        render new JSONObject() as JSON
    }

    def features(String sequenceName, Long organismId, int start, int end) {
        Long timeStart = System.currentTimeMillis()
        log.info "VcfController::features called with ${sequenceName}:${start} - ${end}"
        Organism organism = Organism.findById(organismId)

        JSONObject returnObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, featuresArray)


        String referer = request.getHeader("Referer")
        String refererLoc = trackService.extractLocation(referer)
        log.info "trying to open file from ${organism.directory + "/" + params.urlTemplate}"

        try {
            File file = new File(organism.directory + "/" + params.urlTemplate)
            VCFFileReader vcfFileReader = new VCFFileReader(file)
            MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)
            log.info "Found projection for sequence: ${projection}"
            if (projection) {
                vcfService.processProjection(featuresArray, projection, vcfFileReader, start, end)
            }
            else {
                vcfService.processSequence(featuresArray, sequenceName, vcfFileReader, start, end)
            }
        } catch (FileNotFoundException e) {
            println e.toString()
        }
        Long timeEnd = System.currentTimeMillis()
        log.debug "Time taken to generate data for request to VcfController::features: ${timeEnd - timeStart} ms"
        //log.info "returning with: ${returnObject.toString()}"
        render returnObject as JSON

    }
}
