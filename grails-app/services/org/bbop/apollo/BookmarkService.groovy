package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BookmarkService {

    def permissionService

    Bookmark generateBookmarkForSequence(Sequence... sequences) {
        User user = permissionService.currentUser
        Organism organism = null
        JSONArray sequenceArray = new JSONArray()
        int end = 0;
        for (Sequence seq in sequences) {
            JSONObject sequenceObject = new JSONObject()
            sequenceObject.name = seq.name
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

    List<Sequence> getSequencesFromBookmark(Bookmark bookmark) {
        JSONArray sequeneArray = JSON.parse(bookmark.sequenceList) as JSONArray
        List<String> sequenceNames = []
        for (int i = 0; i < sequeneArray.size(); i++) {
            sequenceNames << sequeneArray.getJSONObject(i).name
        }
        // if unsaved without the organism
        if (bookmark.organism) {
            return Sequence.findAllByOrganismAndNameInList(bookmark.organism, sequenceNames)
        } else {
            Sequence.findAllByNameInList(sequenceNames)
        }
    }

    // should match ProjectionDescription
    JSONObject convertBookmarkToJson(Bookmark bookmark) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.id = bookmark.id
        jsonObject.type = bookmark.type ?: "NONE"
        jsonObject.padding = bookmark.padding ?: 0
        jsonObject.payload = bookmark.payload ?: "{}"
        jsonObject.organism = bookmark.organism.commonName
        jsonObject.start = bookmark.start
        jsonObject.end = bookmark.end
        // in theory these should be the same
        jsonObject.sequenceList = JSON.parse(bookmark.sequenceList) as JSONArray

        return jsonObject
    }

    Bookmark convertJsonToBookmark(JSONObject jsonObject) {
        Bookmark bookmark = new Bookmark()
        bookmark.sequenceList = jsonObject.sequenceList.toString()
        List<Sequence> sequences = getSequencesFromBookmark(bookmark)
        bookmark.organism = sequences.first().organism


        return generateBookmarkForSequence(sequences as Sequence[])
    }

    static Boolean isProjectionString(String inputString ){
        return (inputString.startsWith("{") && inputString.contains("projection"))
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
