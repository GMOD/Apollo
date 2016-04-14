import grails.util.Environment
import org.bbop.apollo.FeatureType
import org.bbop.apollo.Role
import org.bbop.apollo.UserService
import org.bbop.apollo.sequence.SequenceTranslationHandler

class BootStrap {

    def mockupService
    def sequenceService
    def configWrapperService
    def grailsApplication
    def featureTypeService
    def domainMarshallerService
    def proxyService


    def init = { servletContext ->
        log.info "Initializing..."
        def dataSource = grailsApplication.config.dataSource
        log.info "Datasource"
        log.info "Url: ${dataSource.url}"
        log.info "Driver: ${dataSource.driverClassName}"
        log.info "Dialect: ${dataSource.dialect}"

        println "Initializing..."
        def dataSource = grailsApplication.config.dataSource
        println "Datasource"
        println "Url: ${dataSource.url}"
        println "Driver: ${dataSource.driverClassName}"
        println "Dialect: ${dataSource.dialect}"

        domainMarshallerService.registerObjects()
        proxyService.initProxies()

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
