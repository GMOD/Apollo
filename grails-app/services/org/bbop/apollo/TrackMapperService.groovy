package org.bbop.apollo

import org.bbop.apollo.projection.TrackIndex

//import grails.transaction.NotTransactional
//import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

//@Transactional
class TrackMapperService {

    /**
     * Format Organism, Track, JSONArray
     */
    Map<String,Map<String,JSONArray>> tracks = new HashMap<>()

    JSONObject getClass(String organism,String track,Integer index){
        return tracks.get(organism)?.get(track)?.getJSONObject(index)
    }

    List<String> getAttributes(String organism,String track,Integer index){
        JSONArray attributesArray = getClass(organism,track,index)?.getJSONArray("attributes")
        List<String> returnAttributes = []
        for(int i = 0 ; attributesArray && i < attributesArray.size() ; i++){
            returnAttributes << attributesArray.getString(i)
        }
        return returnAttributes
    }

    /**
     * Indicates that it is a "chunk"
     * @return
     */
    Boolean hasSubList(String organism,String track,Integer index){
        // will asume that the key is always 1
        JSONObject rootJsonObject = getClass(organism,track,index)
        if(rootJsonObject.containsKey("isArrayAttr")){
            return rootJsonObject.getJSONObject("isArrayAttr").containsKey("Sublist")
        }
        return false
    }

    /**
     * Indicates if a column has subFeatures
     * @return
     */
    Boolean hasSubFeatures(String organism,String track,Integer index){
        JSONObject rootJsonObject = getClass(organism,track,index)
        if(rootJsonObject.containsKey("isArrayAttr")){
            return rootJsonObject.getJSONObject("isArrayAttr").containsKey("Subfeatures")
        }
        return false
    }

    /**
     * Returns the SubFeatures array if it exists or null
     * @return
     */
    Integer getSubFeaturesIndex(String organism,String track,Integer index){
        if(hasSubFeatures(organism,track,index)){
            return getAttributes(organism,track,index).indexOf("Subfeatures")
        }
        return -1
    }

    def storeTrack(String organismName, String trackName, JSONArray jsonArray) {
        Map<String,JSONArray> organismTracks = tracks.get(organismName) ?: new HashMap<>()
        organismTracks.put(trackName,jsonArray)
    }

    TrackIndex getIndices(String organismName, String trackName, Integer index) {
        List<String> attributes = getAttributes(organismName,trackName,index)
        TrackIndex trackIndex = new TrackIndex()
        trackIndex.start = attributes.indexOf("Start")+1
        trackIndex.end = attributes.indexOf("End")+1
        trackIndex.source = attributes.indexOf("Source")+1
        trackIndex.trackName = trackName
        trackIndex.organism = organismName
        trackIndex.classIndex = index
        return trackIndex
    }
}
