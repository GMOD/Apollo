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

    JSONObject global(String trackName) {
        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = preferenceService.getOrganismFromInput(data)
        if (!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism,trackName)
        JSONObject returnObject = new JSONObject()
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

    def features(String sequenceName, int start, int end) {
        log.info "VcfController::features called with ${sequenceName}:${start} - ${end}"
        JSONObject data = permissionService.handleInput(request, params)
        JSONObject returnObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, featuresArray)
        Organism organism = preferenceService.getCurrentOrganismPreference(permissionService.currentUser,sequenceName,data.getString(FeatureStringEnum.CLIENT_TOKEN.value))?.organism

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

        log.info "returning with: ${returnObject.toString()}"
        render returnObject as JSON

    }
}
