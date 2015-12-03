package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.projection.TrackIndex
import org.bbop.apollo.sequence.Strand
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class TrackController {

    def permissionService
    def trackService
    def sequenceService
    def bookmarkService
    def trackMapperService


    def featureDetail() {
        println "FD params ${params}"
        JSONObject rootElement = new JSONObject()
        rootElement.put(FeatureStringEnum.USERNAME.value, SecurityUtils.subject.principal)
        Organism organism = permissionService.checkPermissionsForOrganism(rootElement, PermissionEnum.READ)
        println "load sequence ! params ${params}"
        println "requestJSON ${request.JSON}"
        String trackName = params.track
        String name = params.name
//        Organism organism = Organism.findById(params.organism)
        Sequence sequence = Sequence.findByOrganismAndName(organism, params.sequence)

        JSONObject jsonObject = retrieveSequence(sequence, trackName, name)

        Integer start = jsonObject.getJSONArray("trackDetails").getInt(1)
        Integer end = jsonObject.getJSONArray("trackDetails").getInt(2)
        Strand strand = Strand.getStrandForValue(jsonObject.getJSONArray("trackDetails").getInt(3))

        String sequenceString = sequenceService.getGenomicResiduesFromSequenceWithAlterations(sequence, start, end, strand)
        render view: "featureDetail", model: [name: params.name, track: params.track, sequence: params.sequence, organism: organism.id, data: jsonObject, sequenceString: sequenceString, start: start, end: end]
    }

    def angularFeatureDetail() {
        println "FD params ${params}"
        JSONObject rootElement = new JSONObject()
        rootElement.put(FeatureStringEnum.USERNAME.value, SecurityUtils.subject.principal)
        Organism organism = permissionService.checkPermissionsForOrganism(rootElement, PermissionEnum.READ)
        render view: "angularFeatureDetail", model: [name: params.name, track: params.track, sequence: params.sequence, organism: organism.id]
    }

    /**
     *
     * Input is track key and projected input.
     * Output is a lookup of name, sequence, etc. to retrieve the proper track data
     *
     *
     * Example input / output
     * [0,291459,294130,1,"amel_OGSv3.2","Group1.10","GB40866-RA",0.763096,"GB40866-RA","mRNA",[[1,291706,291911,1,"amel_OGSv3.2","Group1.10",0,"CDS"],[1,292012,293784,1,"amel_OGSv3.2","Group1.10",2,"CDS"],[2,291459,291595,1,"amel_OGSv3.2","Group1.10",0.763096,"five_prime_UTR"],[2,291696,291706,1,"amel_OGSv3.2","Group1.10",0.763096,"five_prime_UTR"],[2,293784,294130,1,"amel_OGSv3.2","Group1.10",0.763096,"three_prime_UTR"],[2,291459,291595,1,"amel_OGSv3.2","Group1.10",0.763096,"exon"],[2,291696,291911,1,"amel_OGSv3.2","Group1.10",0.763096,"exon"],[2,292012,294130,1,"amel_OGSv3.2","Group1.10",0.763096,"exon"]]]
     */
    def retrieve() {
//        println "request JSON ${request.JSON}"
//        println "params data ${params.data}"
        println "params ${params}"
//        JSONObject requestJson = request.JSON?:JSON.parse(params.data) as JSONObject
        String trackName = params.track
        String organismString = params.organism
        println "organism ${organismString}"
        println "trackName ${trackName}"
        JSONArray inputArray = JSON.parse(params.input) as JSONArray
        println "inputJson ${inputArray as JSON}"


        try {
            String sequenceName = inputArray.getString(5)
            String nameLookup = inputArray.getString(6)

            JSONObject rootElement = new JSONObject()
            rootElement.put(FeatureStringEnum.USERNAME.value, SecurityUtils.subject.principal)
            rootElement.put(FeatureStringEnum.SEQUENCE.value, sequenceName)

            Bookmark bookmark = permissionService.checkPermissions(rootElement, PermissionEnum.READ)

            println "bookmark ${bookmark}"
            assert bookmark != null
            assert bookmark.sequenceList == sequenceName

//            render retrieveSequence(sequence,trackName,nameLookup) as JSON
            render retrieveBookmarkSequence(bookmark, trackName, nameLookup) as JSON
        } catch (e) {
            def error = [error: 'problem retrieving track: ' + e]
            render error as JSON
            e.printStackTrace()
            log.error(error.error)
        }
    }

    /**
     * Used by angular service.
     * @return
     */
    def loadSequence() {
//        JSONObject requestJson = request.JSON?:JSON.parse(params.data) as JSONObject
        println "load sequence ! params ${params}"
        println "requestJSON ${request.JSON}"
        String trackName = params.track
        String name = params.name
        Organism organism = Organism.findById(params.organism)
        Sequence sequence = Sequence.findByOrganismAndName(organism, params.sequence)

        JSONObject jsonObject = retrieveSequence(sequence, trackName, name)
        println "${jsonObject as JSON}"
        render jsonObject as JSON
    }

    private JSONObject retrieveBookmarkSequence(Bookmark bookmark, String trackName, String nameLookup) {
        List<Sequence> sequenceList = bookmarkService.getSequencesFromBookmark(bookmark)
        // TODO: need to merge these!!!
        return retrieveSequence(sequenceList.first(), trackName, nameLookup)
    }


    private JSONObject retrieveSequence(Sequence sequence, String trackName, String nameLookup) {
        JSONArray returnData = trackService.getTrackData(sequence, trackName, nameLookup)
        println "returnData ${returnData as JSON}"

//            render inputArray as JSONArray
        def responseObject = new JSONObject()
        responseObject.organismId = sequence.organismId
        responseObject.trackDetails = returnData

        TrackIndex trackIndex = trackMapperService.getIndices(sequence.organism.commonName, trackName, returnData.getInt(0))

        responseObject.start = returnData.getInt(trackIndex.start)
        responseObject.end = returnData.getInt(trackIndex.end)
        responseObject.strand = returnData.getInt(trackIndex.strand)
        responseObject.note = returnData.getString(trackIndex.source) // not sure if this is correct or not . . .
        responseObject.seq = returnData.getString(trackIndex.seqId) // not sure if this is correct or not . . .
        responseObject.name = returnData.getString(trackIndex.seqId) // not sure if this is correct or not . . .
//        responseObject.name = returnData.getString(6)
        responseObject.score = returnData.getDouble(trackIndex.score)
        responseObject.type = returnData.getString(trackIndex.type)

        return responseObject
    }
}
