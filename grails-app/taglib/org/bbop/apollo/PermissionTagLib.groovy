package org.bbop.apollo

class PermissionTagLib {
//    static defaultEncodeAs = [taglib:'html']
    static defaultEncodeAs = 'raw'
    //static encodeAsForTags = [tagName: 'raw']
    static namespace = 'perms'
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    def permissionService
    def userService


    def admin = { attrs, body ->
        println "${permissionService.currentUser} -> ${permissionService.admin}"
        if (permissionService.admin) {
            out << body()
        }
    }
}
