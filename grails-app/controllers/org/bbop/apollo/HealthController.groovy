package org.bbop.apollo

import grails.converters.JSON

class HealthController {

    def index() {
        if (BootStrap.ready.get()) {
            render([status: 'ready'] as JSON)
        } else {
            response.status = 503
            render([status: 'starting'] as JSON)
        }
    }
}
