package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional

@Transactional
class OrganismService {
    def featureService

    int TRANSACTION_SIZE = 100

    @NotTransactional
    def deleteAllFeaturesForOrganism(Organism organism) {

        def featurePairs = Feature.executeQuery("select f.id,f.uniqueName from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o=:organism", [organism: organism])

        // maximum transaction size  30

        println "feature sublists created ${featurePairs.size()}"
        def featureSubLists = featurePairs.collate(TRANSACTION_SIZE)
        println "sublists size ${featureSubLists.size()}"
        int count = 0
        featureSubLists.each { featureList ->
            def ids = featureList.collect() {
                it[0]
            }
            println "ids ${ids.size()}"
            def uniqueNames = featureList.collect() {
                it[1]
            }
            println "uniqueNames ${uniqueNames.size()}"
            Feature.withTransaction{
                def features = Feature.findAllByIdInList(ids)
                features.each { f ->
                    f.delete()
                }
                def featureEvents = FeatureEvent.findAllByUniqueNameInList(uniqueNames)
                featureEvents.each { fe ->
                    fe.delete()
                }
                organism.save(flush: true)
                count += TRANSACTION_SIZE
                println "count ${count}"
                println "${count} / ${featurePairs.size()}  =  ${count / featurePairs.size()}% "
            }
            println "deleted ${featurePairs.size()}"
            organism.save(flush: true)
        }
        return featurePairs.size()
    }
}
