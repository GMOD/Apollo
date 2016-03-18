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
import org.codehaus.groovy.grails.commons.GrailsApplication

class BootStrap {

    def mockupService
    def sequenceService
    def configWrapperService
    def grailsApplication
    def featureTypeService
    def domainMarshallerService
    def proxyService


    def init = { servletContext ->

        domainMarshallerService.registerObjects()
        proxyService.initProxies()

        SequenceTranslationHandler.spliceDonorSites.addAll(configWrapperService.spliceDonorSites)
        SequenceTranslationHandler.spliceAcceptorSites.addAll(configWrapperService.spliceAcceptorSites)

        println grailsApplication.config.dataSource_chado
        if (configWrapperService.hasChadoDataSource()) {
            if (org.gmod.chado.Cvterm.all.size() == 0) {
                String username = grailsApplication.config.dataSource_chado.username
                String dbName = grailsApplication.config.dataSource_chado.url.split("/")[-1]
                def errorBuffer = new StringBuffer()
                def outputBuffer=  new StringBuffer()
                def proc = "scripts/load_chado.sh ${username} ${dbName}".execute()
                proc.waitForProcessOutput(outputBuffer, errorBuffer)
                log.debug "Error: ${errorBuffer.toString()}"
            }
        }

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
