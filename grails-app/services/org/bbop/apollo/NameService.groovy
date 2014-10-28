package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class NameService {

    def generateUniqueName() {
        UUID.randomUUID().toString()
    }

    def generateUniqueName(Feature thisFeature) {
        UUID.fromString(thisFeature.name).toString()
    }

    def generateUniqueNameFromSource(Feature sourceFeature,Feature thisFeature) {
        UUID.fromString(thisFeature.name+"::"+sourceFeature.name).toString()
    }
}
