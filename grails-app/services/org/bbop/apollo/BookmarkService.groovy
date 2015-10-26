package org.bbop.apollo

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class BookmarkService {

    Bookmark generateBookmarkForSequence(Sequence sequence) {

        JSONArray sequenceArray = new JSONArray()
        JSONObject sequenceObject = new JSONObject()
        sequenceObject.name = sequence.name
        sequenceArray.add(sequenceObject)

        Bookmark bookmark = new Bookmark(
                organism: sequence.organism
                ,sequenceList: sequenceArray.toString()
        ).save(insert: true,flush:true,failOnError: true)

        return bookmark
    }
}
