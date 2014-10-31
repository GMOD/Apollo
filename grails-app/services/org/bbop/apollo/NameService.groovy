package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class NameService {

    String generateUniqueName() {
        UUID.randomUUID().toString()
    }

    String generateUniqueName(Feature thisFeature) {
        UUID.fromString(thisFeature.name).toString()
    }

    String generateUniqueNameFromSource(Feature sourceFeature,Feature thisFeature) {
        UUID.fromString(thisFeature.name+"::"+sourceFeature.name).toString()
    }
}
