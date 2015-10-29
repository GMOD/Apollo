package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BookmarkService {

    Bookmark generateBookmarkForSequence(Sequence... sequences) {

        Organism organism = null
        JSONArray sequenceArray = new JSONArray()
        for(Sequence seq in sequences){
            JSONObject sequenceObject = new JSONObject()
            sequenceObject.name = seq.name
            sequenceArray.add(sequenceObject)
            organism = organism ?: seq.organism
        }

        Bookmark bookmark = new Bookmark(
                organism: organism
                ,sequenceList: sequenceArray.toString()
        ).save(insert: true,flush:true,failOnError: true)

        return bookmark
    }

    List<Sequence> getSequencesFromBookmark(Bookmark bookmark){
        JSONArray sequeneArray = JSON.parse(bookmark.sequenceList) as JSONArray
        List<String> sequenceNames = []
        for(int i = 0 ; i < sequeneArray.size() ; i++){
            sequenceNames << sequeneArray.getJSONObject(i).name
        }
        return Sequence.findAllByOrganismAndNameInList(bookmark.organism,sequenceNames)
    }

    // should match ProjectionDescription
    JSONObject convertBookmarkToJson(Bookmark bookmark) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.type = bookmark.type
        jsonObject.padding = bookmark.padding
        jsonObject.organism = bookmark.organism.commonName
        // in theory these should be the same
        jsonObject.sequenceList = JSON.parse(bookmark.sequenceList) as JSONArray

        return jsonObject
    }
}
