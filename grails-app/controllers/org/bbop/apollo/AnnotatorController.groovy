package org.bbop.apollo

import grails.converters.JSON

class AnnotatorController {

    def index() {}

    def what(String data){
        println params
        println data
        def dataObject = JSON.parse(data)
        println dataObject
        println dataObject.thekey

        render dataObject.thekey
    }
}
