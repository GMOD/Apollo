package org.bbop.apollo

import grails.transaction.Transactional
import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class AnnotatorService {

    def permissionService
    def requestHandlingService
    def bookmarkService

    def getAppState(String token) {
        JSONObject appStateObject = new JSONObject()
        try {
            def organismList = permissionService.getCurrentOrganismPreference(token).organism
            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(permissionService.currentUser, true,token)
            println "found organism preference: ${userOrganismPreference} for token ${token}"
            Long defaultOrganismId = userOrganismPreference ? userOrganismPreference.organism.id : null


            JSONArray organismArray = new JSONArray()
            for (Organism organism in organismList) {
                Integer annotationCount = Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)", [organism: organism, viewableTypes: requestHandlingService.viewableAnnotationList])[0] as Integer
                Integer sequenceCount = Sequence.countByOrganism(organism)
                JSONObject jsonObject = [
                        id             : organism.id as Long,
                        commonName     : organism.commonName,
                        blatdb         : organism.blatdb,
                        directory      : organism.directory,
                        annotationCount: annotationCount,
                        sequences      : sequenceCount,
                        genus          : organism.genus,
                        species        : organism.species,
                        valid          : organism.valid,
                        publicMode     : organism.publicMode,
                        currentOrganism: defaultOrganismId != null ? organism.id == defaultOrganismId : false,
                        editable       : permissionService.userHasOrganismPermission(organism,PermissionEnum.ADMINISTRATE)

                ] as JSONObject
                organismArray.add(jsonObject)
            }
            appStateObject.put("organismList", organismArray)
            UserOrganismPreference currentUserOrganismPreference = permissionService.getCurrentOrganismPreference(token)
            if(currentUserOrganismPreference){
                Organism currentOrganism = currentUserOrganismPreference?.organism
                appStateObject.put("currentOrganism", currentOrganism )


                if (!currentUserOrganismPreference.bookmark) {
                    User currentUser = currentUserOrganismPreference.user
                    // find the first bookmark with a matching organism
                    Bookmark bookmark = bookmarkService.getBookmarksForUserAndOrganism(currentUser,currentOrganism)?.first()
//                    Bookmark bookmark = Bookmark.findByOrganism(currentOrganism,currentUserOrganismPreference.user)
                    if (!bookmark) {
                        // just need the first random one
                        Sequence sequence = Sequence.findByOrganism(currentOrganism)
                        bookmark = bookmarkService.generateBookmarkForSequence(sequence)
                    }
                    currentUserOrganismPreference.bookmark = bookmark
                    currentUserOrganismPreference.save(flush: true)
                }
                appStateObject.put(FeatureStringEnum.CURRENT_BOOKMARK.getValue(), bookmarkService.convertBookmarkToJson(currentUserOrganismPreference.bookmark))


                if (currentUserOrganismPreference.startbp && currentUserOrganismPreference.endbp) {
                    appStateObject.put("currentStartBp", currentUserOrganismPreference.startbp)
                    appStateObject.put("currentEndBp", currentUserOrganismPreference.endbp)
                }
            }
        }
        catch(PermissionException e) {
            def error=[error: "Error: "+e]
            log.error(error.error)
            return error
        }



        return appStateObject
    }
}
