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
    def projectionService
    def bookmarkService

    def list() {
        
        JSONObject bookmarkJson = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject
        User user = permissionService.getCurrentUser(bookmarkJson)
        if(Organism.count>0){
            preferenceService.getCurrentOrganism(user)
            render Bookmark.findAllByUser(user).sort(){ a,b -> a.sequenceList <=> b.sequenceList} as JSON
//            render user.bookmarks.sort(){ a,b -> a.sequenceList <=> b.sequenceList} as JSON
//            render (user.bookmarks ?: new JSONObject()) as JSON
        }
        else{
            render new JSONObject() as JSON
        }

    }

    def getBookmark(){
        JSONObject bookmarkObject = (request.JSON ?: JSON.parse(params.data.toString())) as JSONObject
        User user = permissionService.currentUser
        Organism organism = preferenceService.getCurrentOrganism(user)

        // creates a projection based on the Bookmarks and caches them
        bookmarkObject.organism = organism.commonName
        projectionService.getProjection(bookmarkObject)

        render bookmarkObject as JSON
    }

    @Transactional
    def addBookmark() {
        JSONArray bookmarkArray = (request.JSON ?: JSON.parse(params.data.toString())) as JSONArray
//        User user = permissionService.currentUser
        Bookmark bookmark = bookmarkService.convertJsonToBookmark(bookmarkArray.getJSONObject(0))

        

        

        return list()
    }

    @Transactional
    def deleteBookmark() {
        JSONArray bookmarkJson = (request.JSON ?: JSON.parse(params.data.toString())) as JSONArray
        User user = permissionService.getCurrentUser(new JSONObject())
        Organism organism = preferenceService.getCurrentOrganism(user)
        

        def idList = []
        for(int i = 0 ; i < bookmarkJson.size() ; i++){
            idList.add(bookmarkJson.getJSONObject(i).id)
        }

        Bookmark.deleteAll(Bookmark.findAllByIdInList(idList))

        def bookmarks = Bookmark.findAllByUserAndOrganism(user,organism)
        render bookmarks as JSON
    }

    def searchBookmarks(String searchQuery) {
        JSONArray requestJSONObject = (request.JSON ?: JSON.parse(params.data.toString())) as JSONArray;
        User user = permissionService.currentUser;
        Organism organism = preferenceService.getCurrentOrganism(user);

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
}
