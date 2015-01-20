import org.bbop.apollo.Organism
import org.bbop.apollo.Sequence

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


//        sequenceService.parseRefSeqs()
//        sequenceService.parseAllRefSeqs()
        try {
            Organism.findAllBySequencesIsEmpty(){ organism ->
                println "processing organism ${organism}"
                File testFile = new File(organism.getRefSeqFile())
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
