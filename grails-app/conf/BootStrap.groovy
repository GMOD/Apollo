import org.bbop.apollo.Organism
import org.bbop.apollo.sequence.SequenceTranslationHandler

class BootStrap {

    def mockupService
    def sequenceService
    def configWrapperService

    def init = { servletContext ->
//        if(User.count==0){
//
//        }


        mockupService.addUsers()
        mockupService.addDataAdapters()
        mockupService.addOrganisms()
//        mockupService.addSequences()  // add tracks
//        mockupService.addFeatureWithLocations()  // add tracks

        SequenceTranslationHandler.spliceDonorSites.addAll(configWrapperService.spliceDonorSites)
        SequenceTranslationHandler.spliceAcceptorSites.addAll(configWrapperService.spliceAcceptorSites)

//        sequenceService.parseRefSeqs()
//        sequenceService.parseAllRefSeqs()
        try {


            def c = Organism.createCriteria()
            def results = c.list{
                isEmpty("sequences")
            }

//            results.each{ organism ->
           results.each{ Organism organism ->
                println "processing organism ${organism}"
                 String fileName = organism.getRefseqFile()
                File testFile = new File(fileName)
                if(testFile.exists() && testFile.isFile()){
                    println "trying to load refseq file: ${testFile.absolutePath}"
                    sequenceService.loadRefSeqs(organism)
                }
                else{
                    log.error "file not found: "+testFile.absolutePath
                }
            }
        } catch (e) {
            log.error "Problem loading in external sequences: "+e
        }


    }
    def destroy = {
    }
}
