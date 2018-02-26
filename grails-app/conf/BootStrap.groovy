import org.bbop.apollo.FeatureType
import org.bbop.apollo.Role
import org.bbop.apollo.UserService
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.sequence.SequenceTranslationHandler



class BootStrap {

    def sequenceService
    def configWrapperService
    def grailsApplication
    def featureTypeService
    def domainMarshallerService
    def proxyService
    def userService
    def phoneHomeService


    def init = { servletContext ->
        log.info "Initializing..."
        def dataSource = grailsApplication.config.dataSource
        log.info "Datasource"
        log.info "Url: ${dataSource.url}"
        log.info "Driver: ${dataSource.driverClassName}"
        log.info "Dialect: ${dataSource.dialect}"

        domainMarshallerService.registerObjects()
        proxyService.initProxies()


        SequenceTranslationHandler.spliceDonorSites.addAll(configWrapperService.spliceDonorSites)
        SequenceTranslationHandler.spliceAcceptorSites.addAll(configWrapperService.spliceAcceptorSites)

        if(FeatureType.count==0){
            featureTypeService.stubDefaultFeatureTypes()
        }



        if(Role.count==0){
            def userRole = new Role(name: GlobalPermissionEnum.USER.name(),rank: GlobalPermissionEnum.USER.rank).save()
            userRole.addToPermissions("*:*")
            userRole.removeFromPermissions("cannedComments:*")
            userRole.removeFromPermissions("availableStatus:*")
            userRole.removeFromPermissions("featureType:*")
            def instructorRole = new Role(name: GlobalPermissionEnum.INSTRUCTOR.name(),rank: GlobalPermissionEnum.INSTRUCTOR.rank).save()
            instructorRole.addToPermissions("*:*")
            instructorRole.removeFromPermissions("cannedComments:*")
            instructorRole.removeFromPermissions("availableStatus:*")
            instructorRole.removeFromPermissions("featureType:*")
            def adminRole = new Role(name: GlobalPermissionEnum.ADMIN.name(),rank: GlobalPermissionEnum.ADMIN.rank).save()
            adminRole.addToPermissions("*:*")
        }

        def userRole = Role.findByName(GlobalPermissionEnum.USER.name())
        if(!userRole.rank){
            userRole.rank = GlobalPermissionEnum.USER.rank
            userRole.save()
        }
        def instructorRole = Role.findByName(GlobalPermissionEnum.INSTRUCTOR.name())
        if(!instructorRole){
            instructorRole = new Role(name: GlobalPermissionEnum.INSTRUCTOR.name(),rank: GlobalPermissionEnum.INSTRUCTOR.rank).save()
            instructorRole.addToPermissions("*:*")
            instructorRole.removeFromPermissions("cannedComments:*")
            instructorRole.removeFromPermissions("availableStatus:*")
            instructorRole.removeFromPermissions("featureType:*")
            instructorRole.save()
        }
        def adminRole = Role.findByName(GlobalPermissionEnum.ADMIN.name())
        if(!adminRole.rank){
            adminRole.rank = GlobalPermissionEnum.ADMIN.rank
            adminRole.save()
        }

        def admin = grailsApplication.config?.apollo?.admin
        if(admin){
            userService.registerAdmin(admin.username,admin.password,admin.firstName,admin.lastName)
        }

        phoneHomeService.pingServerAsync(org.bbop.apollo.PhoneHomeEnum.START.value)

    }
    def destroy = {
        phoneHomeService.pingServer(org.bbop.apollo.PhoneHomeEnum.STOP.value)
    }
}
