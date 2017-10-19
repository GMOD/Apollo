package org.bbop.apollo

import grails.converters.JSON
import htsjdk.samtools.BAMFileReader
import htsjdk.samtools.BAMIndex
import htsjdk.samtools.BAMIndexMetaData
import htsjdk.samtools.DefaultSAMRecordFactory
import htsjdk.samtools.SAMSequenceRecord
import htsjdk.samtools.SamInputResource
import htsjdk.samtools.SamReader
import htsjdk.samtools.SamReaderFactory
import htsjdk.samtools.ValidationStringency
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import org.bbop.apollo.gwt.shared.projection.ProjectionSequence
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
//            log.debug "BAM file to read ${file.absolutePath}"
//            log.debug "BAM file to read exists ${file.exists()}"
//            final SamReader samReader = SamReaderFactory.makeDefault().open(SamInputResource.of(file))
            File baiFile = new File(organism.directory + "/" + params.urlTemplate+".bai")
            BAMFileReader samReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())

            MultiSequenceProjection projection = projectionService.getProjection(refererLoc, organism)

            if(projection==null){
                // create a projection from simple sequence
                Sequence sequence = Sequence.findByNameAndOrganism(sequenceName,organism)
                ProjectionSequence projectionSequence = new ProjectionSequence()
                projectionSequence.name = sequence.name
                projectionSequence.start = 0
                projectionSequence.end = sequence.end
                projectionSequence.order = 0
                projectionSequence.reverse = false
                projection = new MultiSequenceProjection()
                projection.addProjectionSequences([projectionSequence])
                projection.addInterval(0l,sequence.end,projectionSequence)
            }
            if (projection) {
                bamService.processProjection(featuresArray, projection, samReader, start, end,file)
            }
            else {
                log.error("Projection not found for ${refererLoc}")
            }
            log.debug "bam feature array size: ${featuresArray.size()}"
        } catch (e) {
            log.error "Error rendering bam track ${e} -> ${file}"
        }

        render returnObject as JSON
    }

    JSONObject regionFeatureDensities(String refSeqName,Long organismId,  Integer start, Integer end, Integer basesPerBin) {

        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        log.debug "params ${params}"

        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        File baiFile = new File(currentOrganism.directory + "/" + trackObject.urlTemplate+".bai")

        JSONObject returnObject = new JSONObject()
        Sequence sequence = Sequence.findByOrganismAndName(currentOrganism,refSeqName)

        BAMFileReader samReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())
        // # of bins: 25?
        int numBins = 25
        // bin size
        int binSize = (end - start ) / numBins
        def binArray = new Integer[numBins]
        int currentStart = start
        int currentEnd = currentStart + binSize
        int min = Integer.MIN_VALUE
        int max = Integer.MAX_VALUE
        for(int i = 0 ; i < numBins ; ++i){
            Integer entrySize = samReader.query(refSeqName,currentStart,currentEnd,false).toList().size()
            binArray[i] = entrySize
            max = entrySize > max ? max : entrySize
            min = entrySize < min ? min : entrySize
        }

        returnObject.bins = new JSONArray(binArray)
        returnObject.stats.basesPerBin = binSize
        returnObject.stats.max = max
        returnObject.stats.min = min


//        BAMIndexMetaData metaData = (bamFileReader.index


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
        render returnObject as JSON
    }

    JSONObject global(String trackName, Long organismId) {

        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        log.debug "params ${params}"

        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        File baiFile = new File(currentOrganism.directory + "/" + trackObject.urlTemplate+".bai")
        BAMFileReader bamFileReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())
        BAMIndexMetaData[] metaData = BAMIndexMetaData.getIndexStats(bamFileReader)
        log.debug "metadata length: " + metaData.length

//        final SamReader reader = SamReaderFactory.makeDefault().open(file)
        JSONObject returnObject = new JSONObject()

        Long featureCount = 0
        // obviously not
        Long totalLength = Organism.executeQuery("select sum(s.length) from Sequence s where s.organism = :organism",[organism:currentOrganism]).first()
        log.debug "total length ${totalLength}"
//        Integer scoreMin = 0
//        Integer scoreMax = 0
//        Integer scoreMean = 0
//        Double scoreStdEv = 0
        metaData.each {
           featureCount += it.alignedRecordCount + it.unalignedRecordCount
        }
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
        returnObject.featureCount = featureCount
        returnObject.featureDensity = (double) featureCount  / (double) totalLength
        log.debug "global BAM ${returnObject as JSON}"

        render returnObject as JSON
    }


    JSONObject region(String refSeqName,Long organismId, Integer start, Integer end) {
        JSONObject data = permissionService.handleInput(request, params)
        Organism currentOrganism = Organism.findById(organismId)
        if(!currentOrganism){
            String clientToken = request.session.getAttribute(FeatureStringEnum.CLIENT_TOKEN.value)
            currentOrganism = preferenceService.getCurrentOrganismForCurrentUser(clientToken)
        }
        Sequence sequence = Sequence.findByNameAndOrganism(refSeqName,currentOrganism)
        JSONObject trackObject = trackService.getTrackObjectForOrganismAndTrack(currentOrganism, trackName)
        log.debug "params ${params}"

        File file = new File(currentOrganism.directory + "/" + trackObject.urlTemplate)
        File baiFile = new File(currentOrganism.directory + "/" + trackObject.urlTemplate+".bai")

        BAMFileReader bamFileReader = new BAMFileReader(file,baiFile,false, false, ValidationStringency.SILENT, new DefaultSAMRecordFactory())
        Long featureCount = bamFileReader.queryAlignmentStart(refSeqName,0).size()
        JSONObject returnObject = new JSONObject()
        returnObject.featureCount = featureCount
        returnObject.featureDensity = (double) featureCount /  (double) sequence.length

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
        log.debug "region BAM ${returnObject as JSON}"
        render returnObject as JSON
    }


}
