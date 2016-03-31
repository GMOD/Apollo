package org.bbop.apollo


import grails.converters.JSON


class ExampleController {

    def index() { }

    def example() {
        render([success: true] as JSON)
    }
}
