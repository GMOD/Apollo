import grails.converters.JSON
import grails.util.Environment
import groovy.lang.Sequence
import org.bbop.apollo.*
import org.bbop.apollo.projection.ProjectionSequence
import org.bbop.apollo.sequence.SequenceTranslationHandler

class BootStrap {

    def mockupService
    def sequenceService
    def configWrapperService
    def grailsApplication
    def featureTypeService


    def init = { servletContext ->

        JSON.registerObjectMarshaller(User) {
            def returnArray = [:]
            returnArray['userId'] = it.id
            returnArray['username'] = it.username
            returnArray['firstName'] = it.firstName
            returnArray['lastName'] = it.lastName
            return returnArray
        }

        JSON.registerObjectMarshaller(Organism) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['commonName'] = it.commonName
            returnArray['genus'] = it?.genus
            returnArray['species'] = it?.species
            returnArray['directory'] = it.directory
            return returnArray
        }

        JSON.registerObjectMarshaller(Sequence) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['length'] = it?.length
            returnArray['start'] = it?.start
            returnArray['end'] = it.end
            return returnArray
        }

        JSON.registerObjectMarshaller(ProjectionSequence) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['organism'] = it.organism
            returnArray['order'] = it.order
            returnArray['offset'] = it.offset
            returnArray['originalOffset'] = it.originalOffset
            returnArray['features'] = it.features?.join("::")
            return returnArray
        }

        JSON.registerObjectMarshaller(Bookmark) {
            def returnArray = [:]
            returnArray['id'] = it?.id
            returnArray['projection'] = it?.projection ?: "NONE"
            returnArray['padding'] = it?.padding ?: 0
            returnArray['payload'] = it?.payload ?: "{}"
            returnArray['start'] = it?.start
            returnArray['end'] = it?.end
            returnArray['sequenceList'] = it?.sequenceList
            return returnArray
        }

        SequenceTranslationHandler.spliceDonorSites.addAll(configWrapperService.spliceDonorSites)
        SequenceTranslationHandler.spliceAcceptorSites.addAll(configWrapperService.spliceAcceptorSites)

        if(FeatureType.count==0){
            featureTypeService.stubDefaultFeatureTypes()
        }

        if(Role.count==0){
            def userRole = new Role(name: UserService.USER).save()
            userRole.addToPermissions("*:*")
            userRole.removeFromPermissions("cannedComments:*")
            userRole.removeFromPermissions("availableStatus:*")
            userRole.removeFromPermissions("featureType:*")
            def adminRole = new Role(name: UserService.ADMIN).save()
            adminRole.addToPermissions("*:*")
        }



        if (grailsApplication.config.apollo.bootstrap || Environment.current == Environment.TEST) {
            log.debug "attempting to bootstrap the data "
            mockupService.bootstrapData()
//            if(grailsApplication.config.apollo.bootstrapClass && grailsApplication.config.apollo.bootstrapMethod){
//                Class.forName(grailsApplication.config.apollo.bootstrapClass).newInstance().invoke(grailsApplication.config.apollo.bootstrapMethod);
//
//            }
        }
        else{
            log.debug "NOT attempting to bootstrap the data "

        }

    }
    def destroy = {
    }
}
