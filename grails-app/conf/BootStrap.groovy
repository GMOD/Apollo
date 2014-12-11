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
            if(Sequence.count==0){
                File testFile = new File(configWrapperService.refSeqDirectory)
                if(testFile.exists() && testFile.isDirectory()){
                    sequenceService.loadRefSeqs(Organism.first(),configWrapperService.refSeqDirectory)
                }
                else{
                    log.error "directory not found: "+configWrapperService.refSeqDirectory
                }
            }
        } catch (e) {
            log.error "Problem loading in external sequences: "+e
        }


    }
    def destroy = {
    }
}
