package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BookmarkService {

    def permissionService
    def preferenceService


    Bookmark generateBookmarkForSequence(Sequence... sequences) {
        Organism organism = null
        JSONArray sequenceArray = new JSONArray()
        int end = 0;
        for (Sequence seq in sequences) {
            // note this creates the proper JSON string
            JSONObject sequenceObject = JSON.parse( (seq as JSON).toString()) as JSONObject
            sequenceArray.add(sequenceObject)
            organism = organism ?: seq.organism
            end += seq.end
        }
        Bookmark bookmark = Bookmark.findByOrganismAndSequenceList(organism, sequenceArray.toString()) ?: new Bookmark(
                organism: organism
                , sequenceList: sequenceArray.toString()
                , start: 0
                , end: end
        ).save(flush: true, failOnError: true)

        return bookmark
    }

    List<Sequence> getSequencesFromBookmark(Organism organism,String sequenceListString) {
        JSONArray sequenceArray = JSON.parse(sequenceListString) as JSONArray
        List<Sequence> sequenceList = []

        for (int i = 0; i < sequenceArray.size(); i++) {
            String sequenceName = sequenceArray.getJSONObject(i).name
            if (organism) {
                sequenceList << Sequence.findByOrganismAndName(organism, sequenceName)
            } else {
                sequenceList << Sequence.findByName(sequenceName)
            }
        }
        return sequenceList
    }

    List<Sequence> getSequencesFromBookmark(Bookmark bookmark) {

        return getSequencesFromBookmark(bookmark.organism,bookmark.sequenceList)
    }

    // should match ProjectionDescription
    JSONObject convertBookmarkToJson(Bookmark bookmark) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.id = bookmark.id
        jsonObject.projection = bookmark.projection ?: "NONE"


        jsonObject.padding = bookmark.padding ?: 0
//        jsonObject.referenceTrack = bookmark.referenceTrack

        jsonObject.payload = bookmark.payload ?: "{}"
        jsonObject.organism = bookmark.organism.commonName
        jsonObject.start = bookmark.start
        jsonObject.end = bookmark.end
        // in theory these should be the same
        jsonObject.sequenceList = JSON.parse(bookmark.sequenceList) as JSONArray

        return jsonObject
    }

    JSONObject standardizeSequenceList(JSONObject inputObject) {
        JSONArray sequenceArray = JSON.parse(inputObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray
        Organism organism = null
        if(inputObject.containsKey(FeatureStringEnum.ORGANISM.value)){
            organism = preferenceService.getOrganismForToken(inputObject.getString(FeatureStringEnum.ORGANISM.value))
        }
        if(!organism){
            UserOrganismPreference userOrganismPreference = preferenceService.getCurrentOrganismPreference(inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            organism = userOrganismPreference?.organism
        }
        Map<String,Sequence> sequenceMap = getSequencesFromBookmark(organism,sequenceArray.toString()).collectEntries(){
            [it.name,it]
        }

        for(int i = 0 ; i < sequenceArray.size() ; i++){
            JSONObject sequenceObject = sequenceArray.getJSONObject(i)
            Sequence sequence = sequenceMap.get(sequenceObject.name)
            sequenceObject.id = sequence.id
            sequenceObject.start = sequenceObject.start ?: sequence.start
            sequenceObject.end = sequenceObject.end ?: sequence.end
            sequenceObject.length = sequenceObject.length ?: sequence.length
        }
        inputObject.put(FeatureStringEnum.SEQUENCE_LIST.value,sequenceArray.toString())

        return inputObject
    }

    def getBookmarksForUserAndOrganism(User user,Organism organism){
        def bookmarks = user.bookmarks.findAll(){
            it.organism==organism
        }
        println "# of bookmarks = ${bookmarks.size()}"
        return bookmarks
    }

    Bookmark convertJsonToBookmark(JSONObject jsonObject) {
        println "convert json to bookmark ${jsonObject as JSON}"
        standardizeSequenceList(jsonObject)
        JSONArray sequenceListArray = JSON.parse(jsonObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)) as JSONArray
        Bookmark bookmark = Bookmark.findBySequenceList(sequenceListArray.toString())
        if(bookmark==null){
            log.info "creating bookmark from ${jsonObject as JSON} "
            bookmark = new Bookmark()
            bookmark.projection = jsonObject.projection
            bookmark.sequenceList = sequenceListArray.toString()

            bookmark.start = jsonObject.containsKey(FeatureStringEnum.START.value) ? jsonObject.getLong(FeatureStringEnum.START.value): sequenceListArray.getJSONObject(0).getInt(FeatureStringEnum.START.value)
            bookmark.end = jsonObject.containsKey(FeatureStringEnum.END.value) ? jsonObject.getLong(FeatureStringEnum.END.value) : sequenceListArray.getJSONObject(sequenceListArray.size()-1).getInt(FeatureStringEnum.END.value)

//            User user = permissionService.getCurrentUser(jsonObject)
//            bookmark.user = userOrganismPreference.user
            bookmark.organism = preferenceService.getOrganismFromInput(jsonObject)
            if(!bookmark.organism){
                bookmark.organism = preferenceService.getCurrentOrganismForCurrentUser(jsonObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            }
            bookmark.save(insert: true,flush:true)
        }
//        else{
//            bookmark.padding = jsonObject.padding
//            bookmark.save(insert:false,flush:true)
//        }

//        return generateBookmarkForSequence(sequences as Sequence[])
        return bookmark
    }

    @NotTransactional
    static Boolean isProjectionReferer(String inputString ){
        return inputString.contains("(")&&inputString.contains("):")&&inputString.contains('..')
    }

    @NotTransactional
    static Boolean isProjectionString(String inputString ){
//        return (inputString.contains("{") && inputString.contains("projection"))
        return (inputString.startsWith("{") && inputString.contains(FeatureStringEnum.SEQUENCE_LIST.value))
    }

}
