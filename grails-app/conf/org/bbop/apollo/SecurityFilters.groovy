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
                        || actionName == "report"
                        || actionName == "loadLink"
                ) {
                    try {
                        log.debug "apollo filter ${controllerName}::${actionName}"
                        Subject subject = SecurityUtils.getSubject();
                        if (!subject.isAuthenticated()) {
                            def req = request.JSON
                            def authToken = req.username ? new UsernamePasswordToken(req.username, req.password) : null
                            // we don't try to add this here
                            if (authToken && permissionService.authenticateWithToken(authToken, request)) {
                                println "authenticated with ${params.targetUri}"
                                if (params.targetUri) {
                                    redirect(uri: params.targetUri)
                                }
                                return true
                            } else {
                                log.warn "Authentication failed"
                                def targetUri = "/${controllerName}/${actionName}"
                                int paramCount = 0
                                def paramString = ""
                                for (p in params) {
                                    if (p.key != "controller" && p.key != "action") {
                                        paramString += paramCount == 0 ? "?" : "&"
                                        if (p.key.startsWith("addStores")) {
                                            paramString += p.key + "=" + URLEncoder.encode(p.value, "UTF-8")
                                        } else {
                                            paramString += p.key + "=" + p.value
                                        }
                                        ++paramCount
                                    }

                                }
                                // https://github.com/GMOD/Apollo/issues/1371
                                // ?ov/Apollo-staging/someanimal/jbrowse/?loc= -> ?loc=
                                // if it contains two question marks with no equals in-between, then fix it
                                // paramString seems to be getting extra data on it via the paramString
                                int indexOfLoc = paramString.indexOf("?loc=")
                                int numberOfStartParams = paramString.findAll("?").size()
                                println "Index of loc string: ${indexOfLoc} "
                                println "Number of start params: ${numberOfStartParams} "
                                println "fixing?: ${paramString} "
                                if (indexOfLoc > 0 && numberOfStartParams>1) {
                                    println "fixing!: ${paramString} "
                                    paramString = paramString.substring(indexOfLoc)
                                }
                                println "target Uri: ${targetUri} AND ${paramString}"
                                targetUri = targetUri + paramString
                                redirect(uri: "/auth/login?targetUri=${targetUri}")
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
