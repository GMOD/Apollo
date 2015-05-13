import grails.converters.JSON
import grails.util.Environment
import org.bbop.apollo.FeatureType
import org.bbop.apollo.Gene
import org.bbop.apollo.MRNA
import org.bbop.apollo.SnRNA
import org.bbop.apollo.Organism
import org.bbop.apollo.Pseudogene
import org.bbop.apollo.Role
import org.bbop.apollo.Transcript
import org.bbop.apollo.User
import org.bbop.apollo.UserService
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

        SequenceTranslationHandler.spliceDonorSites.addAll(configWrapperService.spliceDonorSites)
        SequenceTranslationHandler.spliceAcceptorSites.addAll(configWrapperService.spliceAcceptorSites)

        if(FeatureType.count==0){
            featureTypeService.stubDefaultFeatureTypes()
        }

        if(Role.count==0){
            def userRole = new Role(name: UserService.USER).save()
            userRole.addToPermissions("*:*")
            userRole.removeFromPermissions("cannedComments:*")
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
