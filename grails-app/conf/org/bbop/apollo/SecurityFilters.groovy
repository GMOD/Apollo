package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

class SecurityFilters {

    def permissionService

    def filters = {

        // TODO: this is the right way to do this as it uses proper forwarding, but
        // the user object lacks permissions
//        all(uri: "/**") {
//            before = {
//                // Ignore direct views (e.g. the default main index page).
//                if (!controllerName) return true
//                // Access control by convention.
//                accessControl(auth:false)
//            }
//        }

        // TODO: route more controllers through here
        all(controller: '*', action: '*') {
            before = {
                if (controllerName == "organism"
                        || controllerName == "home"
                        || controllerName == "cannedKey"
                        || controllerName == "cannedValue"
                        || controllerName == "cannedComment"
                        || controllerName == "availableStatus"
                        || controllerName == "proxy"
                        || controllerName == "featureType"
                        || actionName == "report"
                        || actionName == "loadLink"
                ) {
                    try {
                        log.debug "apollo filter ${controllerName}::${actionName}"
                        Subject subject = SecurityUtils.getSubject();
                        if (!subject.isAuthenticated()) {

                            // we don't try to add this here
                            if (permissionService.authenticateWithToken(request)) {
                                if (params.targetUri) {
                                    redirect(uri: params.targetUri)
                                }
                                return true
                            } else {
                                log.warn "Authentication failed"
                                def targetUri = "/${controllerName}/${actionName}"
                                int paramCount = 0
                                def paramString = "?"
                                for(p in request.parameterMap){
                                    if(p.key!="controller" && p.key!="action"){
                                        String key = p.key
                                        // the ?loc is incorrect
                                        // https://github.com/GMOD/Apollo/issues/1371
                                        // ?ov/Apollo-staging/someanimal/jbrowse/?loc= -> ?loc=
                                        if(p.key.contains("?loc")){
                                            def lastIndex = p.key.lastIndexOf("loc")
                                            key = p.key.substring(lastIndex)
                                        }
                                        p.value.each {
                                            paramString += "&${key}=${it}"
                                        }
                                    }

                                }
                                // https://github.com/GMOD/Apollo/issues/1371
                                // ?ov/Apollo-staging/someanimal/jbrowse/?loc= -> ?loc=
                                // if it contains two question marks with no equals in-between, then fix it
                                // paramString seems to be getting extra data on it via the paramString
//                                int indexOfLoc = paramString.indexOf("?loc=")
//                                int numberOfStartParams = paramString.findAll("\\?").size()
//                                if (indexOfLoc > 0 && numberOfStartParams>1) {
//                                    paramString = paramString.substring(indexOfLoc)
//                                }
                                targetUri = targetUri + paramString
                                if (paramString.contains("http://") || paramString.contains("https://") || paramString.contains("ftp://")) {
                                    redirect(uri: "${request.contextPath}/auth/login?targetUri=${targetUri}")
                                }
                                else {
                                    redirect(uri: "/auth/login?targetUri=${targetUri}")
                                }

                                return false
                            }
                        }
                    }
                    catch (Exception e) {
                        render([error: e.message] as JSON)
                        return false
                    }
                }
            }
        }
    }

}
