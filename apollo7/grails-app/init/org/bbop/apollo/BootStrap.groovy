package org.bbop.apollo

import org.bbop.apollo.sequence.SequenceTranslationHandler
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j

import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
class BootStrap {

    static final AtomicBoolean ready = new AtomicBoolean(false)

    SequenceService sequenceService
    ConfigWrapperService configWrapperService
    GrailsApplication grailsApplication
    FeatureTypeService featureTypeService
    DomainMarshallerService domainMarshallerService
    ProxyService proxyService
    UserService userService
    RoleService roleService
    TrackService trackService
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

        ready.set(true)
        log.info "Apollo initialization complete"
    }

    def destroy = {
    }
}
