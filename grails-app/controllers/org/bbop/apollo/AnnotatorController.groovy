package org.bbop.apollo

import grails.converters.JSON

class AnnotatorController {

    def index() {
    }

    def demo() {
    }

    def what(String data){
        println params
        println data
        def dataObject = JSON.parse(data)
        println dataObject
        println dataObject.thekey

        render dataObject.thekey
    }

    def search(String data){
        println params
        println data
        def dataObject = JSON.parse(data)
        println dataObject
        println dataObject.query

        // do stuff
        String result = ['pax6a-001','pax6a-002']

        dataObject.result = result

        println "return object ${dataObject} vs ${dataObject as JSON}"

        render dataObject as JSON
    }
}
