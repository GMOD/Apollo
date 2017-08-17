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

    int TRANSACTION_SIZE = 30

    @NotTransactional
    deleteAllFeaturesForOrganism(Organism organism) {

        def featurePairs = Feature.executeQuery("select f.id,f.uniqueName from Feature f join f.featureLocations fl join fl.sequence s join s.organism o where o=:organism", [organism: organism])
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
        return featurePairs.size()
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

    def unzip(File zipFile, String path, String directoryName = null, boolean tempDir = false) throws IOException {
        log.debug "zipFile: ${zipFile}"
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream((zipFile)))
        ZipEntry entry = zipInputStream.getNextEntry()
        // switching extraction to temp directory, if needed
        String initialLocation = tempDir ? path + File.separator + "temp" : path
        String archiveRoot
        boolean atArchiveRoot = true

        while(entry != null) {
            File file = new File(initialLocation, entry.getName())
            if (atArchiveRoot) {
                atArchiveRoot = false
                archiveRoot = entry.getName()
            }

            if (entry.isDirectory()) {
                if (file.exists()) {
                    throw new IOException("cannot unpack folder ${file} since it already exists")
                }
                file.mkdirs()
            }
            else {
                File parent = file.getParentFile()
                if (!parent.exists()) {
                    parent.mkdirs()
                }

                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file))
                byte[] buffer = new byte[1024]
                int location
                while ((location = zipInputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, location)
                }
                bufferedOutputStream.flush()
                bufferedOutputStream.close()
            }
            entry = zipInputStream.getNextEntry()
        }
        log.debug "extraction complete"
        if (tempDir) {
            // move files from temp directory to folder supplied via name
            String unpackedArchiveLocation = initialLocation + File.separator + archiveRoot
            String finalLocation = path + File.separator + directoryName
            File finalLocationFile = new File(finalLocation)
            if (finalLocationFile.mkdir()) log.debug "[DEBUG][UNZIP] ${finalLocation} directory created"
            try {
                Files.move(new File(unpackedArchiveLocation).toPath(), finalLocationFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                log.debug "[DEBUG][UNZIP] files moved from ${unpackedArchiveLocation} to ${finalLocation}"
            } catch (FileSystemException fse) {
                log.error fse.message
            }

            // delete temp folder
            new File(initialLocation).deleteDir()
        }
    }
}
