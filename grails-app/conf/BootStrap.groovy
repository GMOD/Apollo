class BootStrap {

    def mockupService

    def init = { servletContext ->
//        if(User.count==0){
//
//        }

        mockupService.addUsers()
        mockupService.addDataAdapters()
        mockupService.addOrganisms()
        mockupService.addSequences()  // add tracks
        mockupService.addFeatureWithLocations()  // add tracks


    }
    def destroy = {
    }
}
