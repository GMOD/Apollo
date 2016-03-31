package org.bbop.apolloplugin


import grails.converters.JSON
import org.bbop.apollo.*


class ExampleController {

    def index() { }

    def example() {
        Feature f = new Feature()
        render([features: f] as JSON)
    }
}
