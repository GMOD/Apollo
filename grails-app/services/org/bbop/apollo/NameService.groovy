package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class NameService {

    // TODO: replace with more reasonable naming schemas
    String generateUniqueName() {
        UUID.randomUUID().toString()
    }

//    String generateUniqueName(Feature thisFeature) {
////        UUID.fromString(thisFeature.name.replaceAll("[^a-zA-Z0-9]","")).toString()
//        String sourceString = thisFeature.name.replaceAll("[_\\.0-9]","")
//        println "source string ${sourceString}"
//        UUID.fromString(sourceString).toString()
//    }
//
//    String generateUniqueNameFromSource(Feature sourceFeature,Feature thisFeature) {
//        UUID.fromString(thisFeature.name.replaceFirst("\\W","")+"::"+sourceFeature.name.replaceFirst("\\W","")).toString()
//    }
}
