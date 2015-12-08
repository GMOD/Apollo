package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.projection.NclistColumnEnum
import org.bbop.apollo.projection.TrackIndex

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class TrackMapperService {


    /**
     * Format Organism, Track, JSONArray
     */
    Map<String,Map<String,JSONArray>> tracks = new HashMap<>()

    @NotTransactional
    JSONObject getClass(String organism,String track,Integer index){
        return tracks.get(organism)?.get(track)?.getJSONObject(index)
    }

    @NotTransactional
    List<String> getAttributes(String organism,String track,Integer index){
        JSONObject classObject = getClass(organism,track,index)
        JSONArray attributesArray = classObject?.getJSONArray("attributes")
        List<String> returnAttributes = []
        for(int i = 0 ; attributesArray && i < attributesArray.size() ; i++){
            returnAttributes << attributesArray.getString(i)
        }
        return returnAttributes
    }

    @NotTransactional
    def storeTrack(String organismName, String trackName, JSONArray jsonArray) {
        Map<String,JSONArray> organismTracks = tracks.get(organismName) ?: new HashMap<>()
        organismTracks.put(trackName,jsonArray)
        tracks.put(organismName,organismTracks)
    }

    @NotTransactional
    TrackIndex getIndices(String organismName, String trackName, Integer index) {
        List<String> attributes = getAttributes(organismName,trackName,index)
        TrackIndex trackIndex = new TrackIndex()
        trackIndex.start = attributes.indexOf(NclistColumnEnum.START.value)+1
        trackIndex.end = attributes.indexOf(NclistColumnEnum.END.value)+1
        trackIndex.source = attributes.indexOf(NclistColumnEnum.SOURCE.value)+1
        trackIndex.chunk = attributes.indexOf(NclistColumnEnum.CHUNK.value)+1
        trackIndex.id = attributes.indexOf(NclistColumnEnum.ID.value)+1
        trackIndex.score = attributes.indexOf(NclistColumnEnum.SCORE.value)+1
        trackIndex.seqId = attributes.indexOf(NclistColumnEnum.SEQ_ID.value)+1
        trackIndex.strand = attributes.indexOf(NclistColumnEnum.STRAND.value)+1
        trackIndex.subFeaturesColumn = attributes.indexOf(NclistColumnEnum.SUBFEATURES.value)+1
        trackIndex.sublistColumn = attributes.indexOf(NclistColumnEnum.SUBLIST.value)+1
        trackIndex.type = attributes.indexOf(NclistColumnEnum.TYPE.value)+1
        trackIndex.phase = attributes.indexOf(NclistColumnEnum.PHASE.value)+1

        trackIndex.fixCoordinates()


        trackIndex.trackName = trackName
        trackIndex.organism = organismName
        trackIndex.classIndex = index

        assert trackIndex.start != 0
        assert trackIndex.end != 0

        return trackIndex
    }
}
