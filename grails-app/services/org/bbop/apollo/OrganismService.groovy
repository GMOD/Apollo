package org.bbop.apollo

import grails.transaction.NotTransactional
import grails.transaction.Transactional
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

@Transactional
class OrganismService {

    def featureService
    def configWrapperService

    int MAX_DELETE_SIZE = 10000
    int TRANSACTION_SIZE = 30

    @NotTransactional
    def deleteAllFeaturesForOrganism(Organism organism) {


        int totalDeleted = 0
        println "organism ${organism}"
        def featureCount = Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o=:organism", [organism: organism])[0]
        println "features to delete ${featureCount}"
        while(featureCount>0){
            def featurePairs = Feature.executeQuery("select f.id,f.uniqueName from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o=:organism", [max:MAX_DELETE_SIZE,organism: organism])
            // maximum transaction size  30
            log.debug "feature sublists created ${featurePairs.size()}"
            def featureSubLists = featurePairs.collate(TRANSACTION_SIZE)
            if (!featureSubLists) {
                log.warn("Nothing to delete for ${organism?.commonName}")
                return
            }
            log.debug "sublists size ${featureSubLists.size()}"
            int count = 0
            long startTime = System.currentTimeMillis()
            long endTime
            double totalTime
            featureSubLists.each { featureList ->
                if (featureList) {
                    def ids = featureList.collect() {
                        it[0]
                    }
                    log.info"ids ${ids.size()}"
                    def uniqueNames = featureList.collect() {
                        it[1]
                    }
                    log.debug "uniqueNames ${uniqueNames.size()}"
                    Feature.withNewTransaction{
                        def features = Feature.findAllByIdInList(ids)
                        features.each { f ->
                            f.delete()
                        }
                        def featureEvents = FeatureEvent.findAllByUniqueNameInList(uniqueNames)
                        featureEvents.each { fe ->
                            fe.delete()
                        }
                        organism.save(flush: true)
                        count += featureList.size()
                        log.info "${count} / ${featurePairs.size()}  =  ${100 * count / featurePairs.size()}% "
                    }
                    log.info "deleted ${featurePairs.size()}"
                }
                endTime = System.currentTimeMillis()
                totalTime = (endTime - startTime) / 1000.0f
                startTime = System.currentTimeMillis()
                double rate = featureList.size() / totalTime
                log.info "Deleted ${rate} features / sec"
            }
            totalDeleted += featurePairs.size()

            featureCount = Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o=:organism", [organism: organism])[0]
            println "features remaining to delete ${featureCount} vs deleted ${totalDeleted}"
        }
        return totalDeleted

    }


    TranslationTable getTranslationTable(Organism organism) {
        if(organism?.nonDefaultTranslationTable){
            log.debug "overriding default translation table for ${organism.commonName} with ${organism.nonDefaultTranslationTable}"
            return SequenceTranslationHandler.getTranslationTableForGeneticCode(organism.nonDefaultTranslationTable)
        }
        // just use the default
        else{
            log.debug "using the default translation table"
            return  configWrapperService.getTranslationTable()
        }

    }
}
