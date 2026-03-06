package org.bbop.apollo


import grails.gorm.transactions.Transactional
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum

@Transactional
class RoleService {

    def initRoles() {
        if (Role.count == 0) {
            def userRole = new Role(name: GlobalPermissionEnum.USER.name(), rank: GlobalPermissionEnum.USER.rank).save()
            userRole.addToPermissions("*:*")
            userRole.removeFromPermissions("cannedComments:*")
            userRole.removeFromPermissions("availableStatus:*")
            userRole.removeFromPermissions("featureType:*")
            def instructorRole = new Role(name: GlobalPermissionEnum.INSTRUCTOR.name(), rank: GlobalPermissionEnum.INSTRUCTOR.rank).save()
            instructorRole.addToPermissions("*:*")
            instructorRole.removeFromPermissions("cannedComments:*")
            instructorRole.removeFromPermissions("availableStatus:*")
            instructorRole.removeFromPermissions("featureType:*")
            def adminRole = new Role(name: GlobalPermissionEnum.ADMIN.name(), rank: GlobalPermissionEnum.ADMIN.rank).save()
            adminRole.addToPermissions("*:*")
        }

        def userRole = Role.findByName(GlobalPermissionEnum.USER.name())
        if (!userRole.rank) {
            userRole.rank = GlobalPermissionEnum.USER.rank
            userRole.save()
        }
        def instructorRole = Role.findByName(GlobalPermissionEnum.INSTRUCTOR.name())
        if (!instructorRole) {
            instructorRole = new Role(name: GlobalPermissionEnum.INSTRUCTOR.name(), rank: GlobalPermissionEnum.INSTRUCTOR.rank).save()
            instructorRole.addToPermissions("*:*")
            instructorRole.removeFromPermissions("cannedComments:*")
            instructorRole.removeFromPermissions("availableStatus:*")
            instructorRole.removeFromPermissions("featureType:*")
            instructorRole.save()
        }
        def adminRole = Role.findByName(GlobalPermissionEnum.ADMIN.name())
        if (!adminRole.rank) {
            adminRole.rank = GlobalPermissionEnum.ADMIN.rank
            adminRole.save()
        }
    }
}
