package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional(readOnly = true)
class BookmarkController {

//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService
    def preferenceService
    def projectionService
    def bookmarkService

    def list() {
        JSONObject inputObject = permissionService.handleInput(request,params)
        JSONObject bookmarkObject = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject
        User user = permissionService.getCurrentUser(bookmarkObject)
        if(Organism.count>0){
            Organism currentOrganism = preferenceService.getOrganismFromPreferences(user,null,inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
//            render Bookmark.findAllByUserAndOrganism(user,currentOrganism).sort(){ a,b -> a.sequenceList <=> b.sequenceList} as JSON
            render bookmarkService.getBookmarksForUserAndOrganism(user,currentOrganism).sort(){ a,b -> a.sequenceList <=> b.sequenceList} as JSON
        }
        else{
            render new JSONObject() as JSON
        }

    }

    def getBookmark(){
        JSONObject inputObject = permissionService.handleInput(request,params)
        JSONObject bookmarkObject = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject
        User user = permissionService.currentUser
        Organism organism = preferenceService.getOrganismFromPreferences(user,bookmarkObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value).toString(),inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))

        // creates a projection based on the Bookmarks and caches them
        bookmarkObject.organism = organism.commonName
        projectionService.getProjection(bookmarkObject)

        render bookmarkObject as JSON
    }

    @Transactional
    def addBookmark() {
        JSONObject inputObject = permissionService.handleInput(request,params)
        JSONArray bookmarkArray = (request.JSON ?: JSON.parse(params.data.toString())) as JSONArray
//        User user = permissionService.currentUser
        JSONObject bookmarkJsonObject = bookmarkArray.getJSONObject(0)
        permissionService.copyRequestValues(inputObject,bookmarkJsonObject)
        bookmarkService.convertJsonToBookmark(bookmarkJsonObject) // this will save a new bookmark
        render list() as JSON
    }

    @Transactional
    def addBookmarkAndReturn() {
        JSONObject inputObject = permissionService.handleInput(request,params)
        JSONArray bookmarkArray = (request.JSON ?: JSON.parse(params.data.toString())) as JSONArray
//        User user = permissionService.currentUser
        JSONObject bookmarkJsonObject = bookmarkArray.getJSONObject(0)
        permissionService.copyRequestValues(inputObject,bookmarkJsonObject)
        Bookmark bookmark = bookmarkService.convertJsonToBookmark(bookmarkJsonObject)
        render bookmarkService.convertBookmarkToJson(bookmark) as JSON
    }

    @Transactional
    def deleteBookmark() {
        JSONObject inputObject = permissionService.handleInput(request,params)
        JSONArray bookmarkObject= (request.JSON ?: JSON.parse(params.data.toString())) as JSONArray
        User user = permissionService.getCurrentUser(inputObject)
        Organism organism = preferenceService.getOrganismFromPreferences(user,null,inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
        

        def idList = []
        for(int i = 0 ; i < bookmarkObject.size() ; i++){
            idList.add(bookmarkObject.getJSONObject(i).id)
        }

        Bookmark.deleteAll(Bookmark.findAllByIdInList(idList))


        def bookmarks = Bookmark.findAllByOrganism(organism)
        render bookmarks as JSON
    }

    def searchBookmarks(String searchQuery) {
        JSONObject inputObject = permissionService.handleInput(request,params)
        String clientToken = inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value)
        User user = permissionService.currentUser;
        Organism organism = preferenceService.getOrganismForToken(clientToken);

        ArrayList<Bookmark> bookmarkResults = new ArrayList<Bookmark>();
        for (Bookmark bookmark : user.bookmarks) {
            if (bookmark.sequenceList.toLowerCase().contains(searchQuery)) {
                bookmarkResults.add(bookmark);
            }
        }

        if (bookmarkResults.size() > 0) {
            render bookmarkResults.sort() { a, b -> a.sequenceList <=> b.sequenceList } as JSON
        } else {
            render new JSONObject() as JSON
        }
    }

    def clearBookmarkCache(){
        projectionService.clearProjections()
        render new JSONObject() as JSON
    }
}
