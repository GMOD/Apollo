package org.bbop.apollo

import grails.converters.JSON
import htsjdk.samtools.BAMFileReader
import htsjdk.samtools.SamInputResource
import htsjdk.samtools.SamReader
import htsjdk.samtools.SamReaderFactory
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.nio.file.FileSystems
import java.nio.file.Path

class BamController {

    def permissionService
    def preferenceService
    def sequenceService
    def projectionService
    def assemblageService
    def trackService
    def bamService

    /**
     *{"features": [

     // minimal required data{ "start": 123, "end": 456 },

     // typical quantitative data{ "start": 123, "end": 456, "score": 42 },

     // Expected format of the single feature expected when the track is a sequence data track.{"seq": "gattacagattaca", "start": 0, "end": 14},

     // typical processed transcript with subfeatures{ "type": "mRNA", "start": 5975, "end": 9744, "score": 0.84, "strand": 1,
     "name": "au9.g1002.t1", "uniqueID": "globallyUniqueString3",
     "subfeatures": [{ "type": "five_prime_UTR", "start": 5975, "end": 6109, "score": 0.98, "strand": 1 },{ "type": "start_codon", "start": 6110, "end": 6112, "strand": 1, "phase": 0 },{ "type": "CDS",         "start": 6110, "end": 6148, "score": 1, "strand": 1, "phase": 0 },

     * @param refSeqName The sequence name
     * @param start The request view start
     * @param end The request view end
     * @return
     */
    JSONObject features(String sequenceName,Long organismId, Integer start, Integer end) {

        JSONObject returnObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value, featuresArray)

        Organism organism = Organism.findById(organismId)

        String referer = request.getHeader("Referer")
        String refererLoc = trackService.extractLocation(referer)

        File file
        try {
            file = new File(organism.directory + "/" + params.urlTemplate)
            final SamReader samReader = SamReaderFactory.makeDefault().open(SamInputResource.of(file))

            MultiSequenceProjection projection = projectionService.getProjection(refererLoc, organism)

            if (projection) {
                println "is projectin ${projection}"
                bamService.processProjection(featuresArray, projection, samReader, start, end)
            } else {
                println "NO projectin ${refererLoc}"
                bamService.processSequence(featuresArray, sequenceName, samReader, start, end)
            }
            println "end array ${featuresArray.size()}"
        } catch (e) {
            println "baddness ${e} -> ${file}"
        }

        render returnObject as JSON
    }

    JSONObject region(String refSeqName,Long organismId, Integer start, Integer end) {
        render new JSONObject() as JSON
    }

    JSONObject regionFeatureDensities(String refSeqName,Long organismId,  Integer start, Integer end, Integer basesPerBin) {
//        {
//            "bins":  [ 51, 50, 58, 63, 57, 57, 65, 66, 63, 61,
//                       56, 49, 50, 47, 39, 38, 54, 41, 50, 71,
//                       61, 44, 64, 60, 42
//        ],
//            "stats": {
//            "basesPerBin": 200,
//            "max": 88
//        }
//        }
        render new JSONObject() as JSON
    }

    JSONObject global(String trackName, Long organismId) {
        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        println "params ${params}"

        JSONObject returnObject = new JSONObject()
        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        final SamReader reader = SamReaderFactory.makeDefault().open(file)
//        reader.getFileHeader().
//        reader.query(0,)
//        double mean = reader?.size() > 0 ? reader.sum()/ (double) reader.size() : 0
//        returnObject.put("scoreMin", reader.min())
//        returnObject.put("scoreMax", reader.max())
//        returnObject.put("scoreMean", mean)
////        returnObject.put("scoreStdDev", reader.stdev())
//        returnObject.put("featureCount", reader.size())
//        returnObject.put("featureDensity", 1)
//        {
//
//            "featureDensity": 0.02,
//
//            "featureCount": 234235,
//
//            "scoreMin": 87,
//            "scoreMax": 87,
//            "scoreMean": 42,
//            "scoreStdDev": 2.1
//        }
        render returnObject as JSON
    }

}
