package org.bbop.apollo

import org.bbop.apollo.sequence.SequenceTranslationHandler
import groovy.util.logging.Slf4j

@Slf4j
class BootStrap {

    def sequenceService
    def configWrapperService
    def grailsApplication
    def featureTypeService
    def domainMarshallerService
    def proxyService
    def userService
    def roleService
    def trackService

    def init = { servletContext ->
        log.info "Initializing Apollo..."
        def dataSource = grailsApplication.config.getProperty('dataSource', Map)
        log.info "Datasource URL: ${dataSource?.url}"

        domainMarshallerService.registerObjects()
        proxyService.initProxies()

        SequenceTranslationHandler.spliceDonorSites.addAll(configWrapperService.spliceDonorSites)
        SequenceTranslationHandler.spliceAcceptorSites.addAll(configWrapperService.spliceAcceptorSites)

        if (FeatureType.count == 0) {
            featureTypeService.stubDefaultFeatureTypes()
        }

        roleService.initRoles()

        // BACKWARDS INCOMPATIBILITY: Config access changed.
        // Old: grailsApplication.config?.apollo?.admin
        // New: grailsApplication.config.getProperty('apollo.admin', Map)
        def admin = grailsApplication.config.getProperty('apollo.admin', Map)
        if (admin) {
            userService.registerAdmin(admin.username, admin.password, admin.firstName, admin.lastName)
        }

        trackService.checkCommonDataDirectory()
    }

    def destroy = {
    }
}
