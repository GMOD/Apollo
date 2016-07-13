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
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject)
        if (Organism.count > 0) {
            Organism currentOrganism = preferenceService.getOrganismFromPreferences(user, null, inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))
            render bookmarkService.getBookmarksForUserAndOrganism(user, currentOrganism).sort() { a, b -> a.sequenceList <=> b.sequenceList } as JSON
        } else {
            render new JSONObject() as JSON
        }

    }

    def getBookmark() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.currentUser
        Organism organism = preferenceService.getOrganismFromPreferences(user, inputObject.getJSONArray(FeatureStringEnum.SEQUENCE_LIST.value).toString(), inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))

        // creates a projection based on the Bookmarks and caches them
        inputObject.organism = organism.commonName
        // this generates the projection
        projectionService.getProjection(inputObject)

        render inputObject as JSON
    }

    @Transactional
    def addBookmark() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Bookmark bookmark = bookmarkService.convertJsonToBookmark(inputObject) // this will save a new bookmark
        User user = permissionService.currentUser
        user.addToBookmarks(bookmark)
        user.save(flush: true)
        render list() as JSON
    }

    @Transactional
    def addBookmarkAndReturn() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        Bookmark bookmark = bookmarkService.convertJsonToBookmark(inputObject)
        render bookmarkService.convertBookmarkToJson(bookmark) as JSON
    }

    @Transactional
    def deleteBookmark() {
        JSONObject inputObject = permissionService.handleInput(request, params)
        User user = permissionService.getCurrentUser(inputObject)
        Organism organism = preferenceService.getOrganismFromPreferences(user, null, inputObject.getString(FeatureStringEnum.CLIENT_TOKEN.value))


        def idList = []
//        for(int i = 0 ; i < inputObject.size() ; i++){
//            def obj = inputObject.getJSONObject(i)
        idList.add(inputObject.id)
//        }

        def bookmark = Bookmark.findById(inputObject.id)
        if(bookmark){
            user.removeFromBookmarks(bookmark)
            UserOrganismPreference uop = UserOrganismPreference.findByBookmark(bookmark)
            if(uop){
                if(uop.currentOrganism){
                    log.error("Preference is still current, ignoring")
                    throw new AnnotationException("Can not delete the current bookmark!")
                }
                else{
                    uop.delete()
                }
            }
            bookmark.delete(flush: true)
        }
        else{
            log.error("No bookmark found to delete for ${inputObject as JSON}")
        }

        render list() as JSON
    }

    def searchBookmarks(String searchQuery) {
        JSONObject inputObject = permissionService.handleInput(request, params)
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

    def clearBookmarkCache() {
        projectionService.clearProjections()
        render new JSONObject() as JSON
    }
}
