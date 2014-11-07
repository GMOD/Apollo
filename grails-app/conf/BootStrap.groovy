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
        if(Sequence.count==0){
            sequenceService.loadRefSeqs(Organism.first(),configWrapperService.refSeqDirectory)
        }


    }
    def destroy = {
    }
}
