package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.gmod.chado.ApplicationMetadata

@Transactional
class ChadoHandlerService {

    def writeFeatures(Organism organism,Collection<Feature> features){

        JSONObject jsonObject = JSON.parse(organism.metadata)

//        org.gmod.chado.Feature oldFeatures =

        if(!jsonObject.containsKey("chado")){
            log.error("No chado database specified")
            return
        }
        JSONObject chado = jsonObject.chado


        // identify the features not yet in Chado
        Collection<Long> featureIds = features.id


        ApplicationMetadata applicationMetadata = ApplicationMetadata.findByKey("lastUpdate")
        Date lastUpdatedDate = null
        if(applicationMetadata){
           lastUpdatedDate = Date.parse("YYYY-MM-DD",applicationMetadata.value)
        }


        List<org.gmod.chado.Feature> chadoNewFeatures = org.gmod.chado.Feature.findAllByIdNotInList(featureIds)
        List<org.gmod.chado.Feature> chadoUpdatedFeatures = org.gmod.chado.Feature.findAllByIdInList(featureIds)


        addFeatures(chadoNewFeatures)
        updateFeatures(chadoUpdatedFeatures)

//        deleteFeatures(chadoOldFeatures)

    }

    def deleteFeatures(List<org.gmod.chado.Feature> features) {
        null
    }

    def updateFeatures(List<org.gmod.chado.Feature> features) {

    }

    def addFeatures(List<org.gmod.chado.Feature> features) {

    }
}
