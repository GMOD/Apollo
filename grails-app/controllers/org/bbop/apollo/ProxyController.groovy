package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class ProxyController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def permissionService
    def proxyService

    def beforeInterceptor = {
        if (actionName != "request" && !permissionService.checkPermissions(PermissionEnum.ADMINISTRATE)) {
            forward action: "notAuthorized", controller: "annotator"
            return
        }
    }

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Proxy.list(params), model: [proxyInstanceCount: Proxy.count()]
    }

    def show(Proxy proxyInstance) {
        respond proxyInstance
    }

    def create() {
        params.active = true
        respond new Proxy(params)
    }

    /**
     * @return
     */
    @Transactional
    def request(String url) {
        // only a logged-in user can use the proxy
        User currentUser = permissionService.currentUser
        if (!currentUser) {
            log.warn "Attempting to proxy ${url} without a logged-in user"
            render status: UNAUTHORIZED
            return
        }
        String referenceUrl = URLDecoder.decode(url, "UTF-8")
        Proxy proxy = proxyService.findProxyForUrl(referenceUrl)


        if (!proxy) {
            log.error "Proxy not found for ${referenceUrl}.  Please add a proxy (see the config guide)."
            render status: NOT_FOUND
            return
        }

        log.info "using proxy ${proxy?.targetUrl}"

        String targetUrl = proxy ? proxy.targetUrl : referenceUrl

        targetUrl += "?" + request.queryString
        log.debug "target url: ${targetUrl}"
        URL returnUrl = new URL(targetUrl)

        log.debug "input URI ${request.requestURI}"
        log.info "request url ${referenceUrl}?${request.getQueryString()}"
        log.info "return url: ${returnUrl}"
        render text: returnUrl.text
    }


    @Transactional
    def save(Proxy proxyInstance) {
        if (proxyInstance == null) {
            notFound()
            return
        }

        if (proxyInstance.hasErrors()) {
            respond proxyInstance.errors, view: 'create'
            return
        }

        proxyInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'proxy.label', default: 'Proxy'), proxyInstance.id])
                redirect proxyInstance
            }
            '*' { respond proxyInstance, [status: CREATED] }
        }
    }

    def edit(Proxy proxyInstance) {
        respond proxyInstance
    }

    @Transactional
    def update(Proxy proxyInstance) {
        if (proxyInstance == null) {
            notFound()
            return
        }

        if (proxyInstance.hasErrors()) {
            respond proxyInstance.errors, view: 'edit'
            return
        }

        proxyInstance.save flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'Proxy.label', default: 'Proxy'), proxyInstance.id])
                redirect proxyInstance
            }
            '*' { respond proxyInstance, [status: OK] }
        }
    }

    @Transactional
    def delete(Proxy proxyInstance) {

        if (proxyInstance == null) {
            notFound()
            return
        }

        proxyInstance.delete flush: true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'Proxy.label', default: 'Proxy'), proxyInstance.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'proxy.label', default: 'Proxy'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }
}
