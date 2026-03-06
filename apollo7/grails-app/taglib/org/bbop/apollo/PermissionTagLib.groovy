package org.bbop.apollo

class PermissionTagLib {
//    static defaultEncodeAs = [taglib:'html']
    static defaultEncodeAs = 'raw'
    //static encodeAsForTags = [tagName: 'raw']
    static namespace = 'perms'
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    def permissionService

    def isUserAdmin = { attrs, body ->
        if (permissionService.isUserGlobalAdmin(attrs.user)) {
            out << body()
        }
    }

    def isUserNotAdmin = { attrs, body ->
        if (!permissionService.isUserGlobalAdmin(attrs.user)) {
            out << body()
        }
    }

    def admin = { attrs, body ->
        if (permissionService.admin) {
            out << body()
        }
    }
}
