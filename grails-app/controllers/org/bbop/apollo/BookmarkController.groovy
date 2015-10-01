package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class BookmarkController {

//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService
    def preferenceService

    def list() {
        println "loading bookmark . . . "
        JSONObject bookmarkJson = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject
        User user = permissionService.getCurrentUser(bookmarkJson)
        Organism organism = preferenceService.getCurrentOrganism(user)

        def bookmarks = UserBookmark.findAllByUserAndOrganism(user,organism)

        render bookmarks as JSON
    }

    @Transactional
    def addBookmark() {
        JSONObject bookmarkJson = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject
        User user = permissionService.getCurrentUser(bookmarkJson)
        Organism organism = preferenceService.getCurrentOrganism(user)

        UserBookmark userBookmark = new UserBookmark(
                id: bookmarkJson.id
                , user: user
                , type: bookmarkJson.type
                , padding: bookmarkJson.padding
                , payload: bookmarkJson.payload
                , organism: organism
                , sequenceList: (bookmarkJson.sequenceList as JSON).toString()
        ).save()

        println "output ${userBookmark as JSON}"
        println "is valid ${userBookmark.validate()}"


        render list() as JSON
    }

    @Transactional
    def deleteBookmark() {
        JSONObject bookmarkJson = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject


        render list() as JSON
    }
}
