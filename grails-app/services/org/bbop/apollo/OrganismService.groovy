package org.bbop.apollo

import grails.gorm.transactions.Transactional
import groovy.io.FileType
import org.springframework.transaction.annotation.Propagation
import org.bbop.apollo.gwt.shared.FeatureStringEnum
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

    FeatureService featureService
    ConfigWrapperService configWrapperService
    int MAX_DELETE_SIZE = 10000
    int TRANSACTION_SIZE = 30

    /**
     * If file path contains "searchDatabaseData"
     * @param path
     * @return
     */
    String findBlatDB(String path){
        String searchDatabaseDirectory = path + "/" + FeatureStringEnum.SEARCH_DATABASE_DATA.value
        File searchFile = new File(searchDatabaseDirectory)
        if(searchFile.exists()){
            String returnFile = null
            searchFile.eachFileRecurse(FileType.FILES) {
                if(it.name.endsWith(".2bit")){
                    returnFile = it
                }
            }
            return returnFile
        }

        return null

    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    def deleteAllFeaturesForSequences(List<Sequence> sequences) {

        int totalDeleted = 0
        def featureCount = Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s where s in (:sequenceList)", [sequenceList: sequences])[0]
        log.debug "features to delete ${featureCount}"
        while(featureCount>0){
            def featurePairs = Feature.executeQuery("select f.id,f.uniqueName from Feature f join f.featureLocations fl join fl.sequence s where s in (:sequenceList)", [max:MAX_DELETE_SIZE,sequenceList: sequences])
            // maximum transaction size  30
            log.debug "feature sublists created ${featurePairs.size()}"
            def featureSubLists = featurePairs.collate(TRANSACTION_SIZE)
            if (!featureSubLists) {
                log.warn("Nothing to delete")
                return
            }
            log.debug "sublists size ${featureSubLists.size()}"
            int count = 0
            long startTime = System.currentTimeMillis()
            long endTime
            double totalTime
            for (featureList in featureSubLists) {
                if (featureList) {
                    def ids = new ArrayList()
                    for (pair in featureList) { ids.add(pair[0]) }
                    log.info"ids ${ids.size()}"
                    def uniqueNames = new ArrayList()
                    for (pair in featureList) { uniqueNames.add(pair[1]) }
                    log.debug "uniqueNames ${uniqueNames.size()}"
                    Feature.withNewTransaction{
                        def features = Feature.findAllByIdInList(ids)
                        log.debug "Found ${features.size()} features for deletion"
                        for (f in features) {
                            f.delete(flush: true)
                        }
                        def featureEvents = FeatureEvent.findAllByUniqueNameInList(uniqueNames)
                        for (fe in featureEvents) {
                            fe.delete(flush: true)
                        }
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

            featureCount = Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s where s in (:sequenceList)", [sequenceList: sequences])[0]
            log.debug "features remaining to delete ${featureCount} vs deleted ${totalDeleted}"
        }
        return totalDeleted

    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    def deleteAllFeaturesForOrganism(Organism organism) {

        int totalDeleted = 0
        log.debug "organism ${organism}"
        def featureCount = Feature.executeQuery("select count(f) from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o=:organism", [organism: organism])[0]
        log.info "features to delete ${featureCount} for organism ${organism.commonName}"
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
            for (featureList in featureSubLists) {
                if (featureList) {
                    def ids = new ArrayList()
                    for (pair in featureList) { ids.add(pair[0]) }
                    log.info"ids ${ids.size()}"
                    def uniqueNames = new ArrayList()
                    for (pair in featureList) { uniqueNames.add(pair[1]) }
                    log.debug "uniqueNames ${uniqueNames.size()}"
                    Feature.withNewTransaction{
                        def features = Feature.findAllByIdInList(ids)
                        log.debug "Found ${features.size()} features for deletion"
                        for (f in features) {
                            f.delete(flush: true)
                        }
                        def featureEvents = FeatureEvent.findAllByUniqueNameInList(uniqueNames)
                        for (fe in featureEvents) {
                            fe.delete(flush: true)
                        }
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
            log.debug "features remaining to delete ${featureCount} vs deleted ${totalDeleted}"
        }
        return totalDeleted

    }


    TranslationTable getTranslationTable(Organism organism) {
        if(organism?.nonDefaultTranslationTable){
            log.debug "overriding default translation table for ${organism.commonName} with ${organism.nonDefaultTranslationTable}"
            return SequenceTranslationHandler.getTranslationTableForGeneticCode(organism.nonDefaultTranslationTable,configWrapperService.getWebRootDir())
        }
        // just use the default
        else{
            log.debug "using the default translation table"
            return  configWrapperService.getTranslationTable()
        }

    }
}
