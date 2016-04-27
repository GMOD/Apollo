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


    Bookmark generateBookmarkForSequence(User user,Sequence... sequences) {
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
        Bookmark bookmark = Bookmark.findByOrganismAndSequenceListAndUser(organism, sequenceArray.toString(), user) ?: new Bookmark(
                organism: organism
                , sequenceList: sequenceArray.toString()
                , start: 0
                , end: end
                , user: user
        ).save(insert: true, flush: true, failOnError: true)

        return bookmark
    }

    Bookmark generateBookmarkForSequence(Sequence... sequences) {
        User user = permissionService.currentUser
        return sequences ? generateBookmarkForSequence(user,sequences) : null
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
        UserOrganismPreference userOrganismPreference = permissionService.currentOrganismPreference
        Map<String,Sequence> sequenceMap = getSequencesFromBookmark(userOrganismPreference.organism,sequenceArray.toString()).collectEntries(){
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


    Bookmark convertJsonToBookmark(JSONObject jsonObject) {
        standardizeSequenceList(jsonObject)
        String sequenceListString = jsonObject.getString(FeatureStringEnum.SEQUENCE_LIST.value)
        Bookmark bookmark = Bookmark.findBySequenceList(sequenceListString)
        if(bookmark==null){
            log.info "creating bookmark from ${jsonObject as JSON} "
            bookmark = new Bookmark()
            bookmark.projection = jsonObject.projection
            bookmark.sequenceList = sequenceListString

            bookmark.start = jsonObject.getLong(FeatureStringEnum.START.value)
            bookmark.end = jsonObject.getLong(FeatureStringEnum.END.value)

            UserOrganismPreference userOrganismPreference = permissionService.currentOrganismPreference
            bookmark.user = userOrganismPreference.user
            bookmark.organism = userOrganismPreference.organism
            bookmark.save(insert: true,flush:true)
        }
        else{
            bookmark.padding = jsonObject.padding
            bookmark.save(insert:false,flush:true)
        }

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

    Bookmark convertStringToBookmark(String inputString, Organism organism) {
        if (isProjectionString(inputString)) {
            JSONObject jsonObject = JSON.parse(inputString) as JSONObject
            return convertJsonToBookmark(jsonObject)
        } else {
            Sequence sequence = Sequence.findByNameAndOrganism(inputString, organism)
            return generateBookmarkForSequence(sequence)
        }
    }
}
