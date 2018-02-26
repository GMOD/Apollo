package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.apache.commons.collections.map.MultiKeyMap
import org.bbop.apollo.gwt.shared.track.NclistColumnEnum
import org.bbop.apollo.gwt.shared.track.TrackIndex
import org.bbop.apollo.sequence.SequenceDTO
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class TrackMapperService {


    /**
     * Format Organism, Track, JSONArray
     */
    MultiKeyMap tracks = new MultiKeyMap()


    @NotTransactional
    List<String> getAttributes(SequenceDTO sequenceDTO, Integer index){
        JSONArray classArray = tracks.get(sequenceDTO.organismCommonName,sequenceDTO.trackName,sequenceDTO.sequenceName)
        JSONObject classObject =classArray.getJSONObject(index)
        JSONArray attributesArray = classObject?.getJSONArray("attributes")
        List<String> returnAttributes = []
        for(int i = 0 ; attributesArray && i < attributesArray.size() ; i++){
            returnAttributes << attributesArray.getString(i)
        }
        return returnAttributes
    }

    @NotTransactional
    def storeTrack(SequenceDTO sequenceDTO, JSONArray jsonArray) {
        tracks.put(sequenceDTO.organismCommonName,sequenceDTO.trackName,sequenceDTO.sequenceName,jsonArray)
    }

    @NotTransactional
    TrackIndex getIndices(SequenceDTO sequenceDTO, Integer index) {
        List<String> attributes = getAttributes(sequenceDTO,index)
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
        trackIndex.name = attributes.indexOf(NclistColumnEnum.NAME.value)+1
        trackIndex.alias = attributes.indexOf(NclistColumnEnum.ALIAS.value)+1
        trackIndex.fixCoordinates()


        trackIndex.trackName = sequenceDTO.trackName
        trackIndex.organism = sequenceDTO.organismCommonName
        trackIndex.classIndex = index

        assert trackIndex.start != 0
        assert trackIndex.end != 0

        return trackIndex
    }
}
