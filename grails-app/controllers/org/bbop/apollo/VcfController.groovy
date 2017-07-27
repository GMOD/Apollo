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

    public static final String TYPE_HISTOGRAM = "histogram"
    public static final String TYPE_FEATURES = "features"

    def index() { }

    JSONObject global(String trackName, Long organismId) {
        // TODO
        JSONObject returnObject = new JSONObject()
        returnObject.put("featureDensity", 0.2);
        render returnObject as JSON
    }

    def region(String trackName, Long organismId, String sequenceName) {
        log.info "trackName: ${trackName}"
        log.info "organismId: ${organismId}"
        int start = params.getInt("start")
        int end = params.getInt("end")
        Organism organism = Organism.findById(organismId)
        JSONObject returnObject = new JSONObject()

        try {
            File file = new File(organism.directory + "/" + params.urlTemplate)
            VCFFileReader vcfFileReader = new VCFFileReader(file)
            MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)
            if (projection) {
                vcfService.getRegionStats(returnObject, organism, projection, vcfFileReader, start, end)
            }
            else {
                vcfService.getRegionStats(returnObject, organism, sequenceName, vcfFileReader, start, end)
            }
        } catch(Exception e) {
            println e.toString()
        }

        render returnObject as JSON

    }

    def regionFeatureDensities(String trackName, Long organismId, String sequenceName) {
        Organism organism = Organism.findById(organismId)
        int start = params.getInt("start")
        int end = params.getInt("end")
        int basesPerBin = params.getInt("basesPerBin")
        int numBins = 25
        JSONObject returnObject = new JSONObject()
        JSONObject statsJsonObject = new JSONObject()
        JSONArray binsArray = new JSONArray()

        returnObject = trackService.getTrackDataFromCache(organism, sequenceName, start, end, params.urlTemplate, TYPE_HISTOGRAM) ?: new JSONObject()
        if (returnObject.containsKey("bins")) {
            log.debug "Cache found for request; returning cached histogram data for ${sequenceName}:${start}..${end} ${params.urlTemplate}"
        }
        else {
            try {
                File file = new File(organism.directory + "/" + params.urlTemplate)
                VCFFileReader vcfFileReader = new VCFFileReader(file)
                MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)
                if (projection) {
                    vcfService.getFeatureDensitiesForRegion(binsArray, organism, projection, vcfFileReader, start, end, numBins, basesPerBin)
                }
                else {
                    vcfService.getFeatureDensitiesForRegion(binsArray, organism, sequenceName, vcfFileReader, start, end, numBins, basesPerBin)
                }
            } catch (FileNotFoundException e) {
                println e.toString()
            }

            statsJsonObject.put("basesPerBin", basesPerBin)
            statsJsonObject.put("max", binsArray.max())
            returnObject.put("bins", binsArray)
            returnObject.put("stats", statsJsonObject)
            log.debug "Caching histogram data for ${sequenceName}:${start}..${end} ${params.urlTemplate}"
            trackService.cacheTrackData(returnObject, organism, sequenceName, start, end, params.urlTemplate, TYPE_HISTOGRAM)
        }

        render returnObject as JSON
    }

    def features(String sequenceName, Long organismId, int start, int end) {
        Long timeStart = System.currentTimeMillis()
        log.info "VcfController::features called with ${sequenceName}:${start} - ${end}"
        Organism organism = Organism.findById(organismId)

        JSONObject returnObject = new JSONObject()
        returnObject = trackService.getTrackDataFromCache(organism, sequenceName, start, end, params.urlTemplate, TYPE_FEATURES) ?: new JSONObject()
        if (returnObject.containsKey(FeatureStringEnum.FEATURES.value)) {
            log.debug "Cache found for request; returning cached data for ${sequenceName}:${start}..${end} ${params.urlTemplate}"
        }
        else {
            JSONArray featuresArray = new JSONArray()
            returnObject.put(FeatureStringEnum.FEATURES.value, featuresArray)
            try {
                File file = new File(organism.directory + "/" + params.urlTemplate)
                VCFFileReader vcfFileReader = new VCFFileReader(file)
                MultiSequenceProjection projection = projectionService.getProjection(sequenceName, organism)
                log.info "Found projection for sequence: ${projection}"
                if (projection) {
                    vcfService.processProjection(featuresArray, organism, projection, vcfFileReader, start, end)
                }
                else {
                    vcfService.processSequence(featuresArray, organism, sequenceName, vcfFileReader, start, end)
                }
            } catch (FileNotFoundException e) {
                println e.toString()
            }
            Long timeEnd = System.currentTimeMillis()
            log.debug "Time taken to generate data for request to VcfController::features: ${timeEnd - timeStart} ms"
            log.debug "Caching data for ${sequenceName}:${start}..${end} ${params.urlTemplate}"
            trackService.cacheTrackData(returnObject, organism, sequenceName, start, end, params.urlTemplate, TYPE_FEATURES)
        }

        render returnObject as JSON
    }

    def getVcfHeader(String trackName, Long organismId, String sequenceName) {
        Organism organism = Organism.findById(organismId)
        JSONObject vcfHeaderJSONObject = new JSONObject()

        try {
            File file = new File(organism.directory + "/" + params.urlTemplate)
            VCFFileReader vcfFileReader = new VCFFileReader(file)
            vcfHeaderJSONObject = vcfService.parseVcfFileHeader(vcfFileReader.getFileHeader())
        } catch(FileNotFoundException e) {
            println e.toString()
        }

        render vcfHeaderJSONObject as JSON
    }

}
