package org.bbop.apollo

import edu.unc.genomics.Interval
import edu.unc.genomics.io.BigWigFileReader
import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import java.nio.file.FileSystems
import java.nio.file.Path

class BigwigController {

    def permissionService
    def preferenceService
    def sequenceService

    /**
     * {
     "features": [

     // minimal required data
    { "start": 123, "end": 456 },

    // typical quantitative data
    { "start": 123, "end": 456, "score": 42 },

    // Expected format of the single feature expected when the track is a sequence data track.
    {"seq": "gattacagattaca", "start": 0, "end": 14},

    // typical processed transcript with subfeatures
    { "type": "mRNA", "start": 5975, "end": 9744, "score": 0.84, "strand": 1,
        "name": "au9.g1002.t1", "uniqueID": "globallyUniqueString3",
        "subfeatures": [
            { "type": "five_prime_UTR", "start": 5975, "end": 6109, "score": 0.98, "strand": 1 },
            { "type": "start_codon", "start": 6110, "end": 6112, "strand": 1, "phase": 0 },
            { "type": "CDS",         "start": 6110, "end": 6148, "score": 1, "strand": 1, "phase": 0 },

     * @param refSeqName
     * @param start
     * @param end
     * @return
     */
    JSONObject features(String sequenceName, Integer start, Integer end) {
        println "params ${params}"
        println "refSeqName ${sequenceName}, start ${start}, end ${end}"

        JSONObject returnObject = new JSONObject()
        JSONArray featuresArray = new JSONArray()
        returnObject.put(FeatureStringEnum.FEATURES.value,featuresArray)

        BigWigFileReader bigWigFileReader
        Path path
        try {
            path = FileSystems.getDefault().getPath(getJBrowseDirectoryForSession()+"/"+params.urlTemplate)
            // TODO: should cache these if open
            bigWigFileReader = new BigWigFileReader(path)

            Integer chrStart = bigWigFileReader.getChrStart(sequenceName)
            Integer chrStop = bigWigFileReader.getChrStop(sequenceName)
            double mean = bigWigFileReader.mean()
            double max = bigWigFileReader.max()
            double min = bigWigFileReader.min()

            Interval interval = new Interval(sequenceName,chrStart,chrStop)
            edu.unc.genomics.Contig contig = bigWigFileReader.query(interval)
            float[] values = contig.get(interval)
            println "input values ${min}, ${mean}, ${max}"
            println "length ${chrStop-chrStart}, values ${values.length}"


            Integer actualStart = chrStart + start
            Integer actualStop = chrStart + end
            println "chr start ${chrStart}"
            println "chr stop ${chrStop}"
            println "actual start ${actualStart}"
            println "actual stop ${actualStop}"

            // let 500 be max in view
            int maxInView = 500
            // calculate step size
            int stepSize = maxInView < (actualStop - actualStart) ? (actualStop-actualStart) / maxInView : 1

            println "step size ${stepSize} "
            println " -> steps: ${(actualStop-actualStart)/stepSize}"


            for(int i = actualStart; i < actualStop ; i+=stepSize ){
                JSONObject globalFeature = new JSONObject()
                globalFeature.put("start",i)
                globalFeature.put("end",i+1)
                if(values[i]<max && values[i]>min){
                    globalFeature.put("score",values[i])
                    featuresArray.add(globalFeature)
                }
            }

            println "end array ${featuresArray.size()}"



        } catch (e) {
            println "baddness ${e} -> ${path}"
        }





        render returnObject as JSON
    }

    JSONObject region(String refSeqName, Integer start, Integer end) {
        render new JSONObject() as JSON
    }

    JSONObject regionFeatureDensities(String refSeqName, Integer start, Integer end, Integer basesPerBin) {
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

    JSONObject global(){
        println "params ${params}"

        JSONObject returnObject = new JSONObject()
        Path path = FileSystems.getDefault().getPath(getJBrowseDirectoryForSession()+"/"+params.urlTemplate)
        // TODO: should cache these if open
        BigWigFileReader bigWigFileReader = new BigWigFileReader(path)
        returnObject.put("scoreMin",bigWigFileReader.min())
        returnObject.put("scoreMax",bigWigFileReader.max())
        returnObject.put("scoreMean",bigWigFileReader.mean())
        returnObject.put("scoreStdDev",bigWigFileReader.stdev())
        returnObject.put("featureCount",bigWigFileReader.numBases())
        returnObject.put("featureDensity",1)
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

    // TODO: abstract or put in service
    private String getJBrowseDirectoryForSession() {
        if (!permissionService.currentUser) {
            return request.session.getAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value)
        }

        String organismJBrowseDirectory = preferenceService.currentOrganismForCurrentUser.directory
        if (!organismJBrowseDirectory) {
            for (Organism organism in Organism.all) {
                // load if not
                if (!organism.sequences) {
                    sequenceService.loadRefSeqs(organism)
                }

                if (organism.sequences) {
                    User user = permissionService.currentUser
                    UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganism(user, organism)
                    Sequence sequence = organism?.sequences?.first()
                    if (userOrganismPreference == null) {
                        userOrganismPreference = new UserOrganismPreference(
                                user: user
                                , organism: organism
                                , sequence: sequence
                                , currentOrganism: true
                        ).save(insert: true, flush: true)
                    } else {
                        userOrganismPreference.sequence = sequence
                        userOrganismPreference.currentOrganism = true
                        userOrganismPreference.save()
                    }

                    organismJBrowseDirectory = organism.directory
                    session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organismJBrowseDirectory)
                    session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequence.name)
                    session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, sequence.organismId)
                    session.setAttribute(FeatureStringEnum.ORGANISM.value, sequence.organism.commonName)
                    return organismJBrowseDirectory
                }
            }
        }
        return organismJBrowseDirectory
    }
}
