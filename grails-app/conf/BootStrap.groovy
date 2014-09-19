class BootStrap {

    def mockupService

    def init = { servletContext ->
//        if(User.count==0){
//
//        }

        mockupService.addUsers()
        mockupService.addDataAdapters()
        mockupService.addTerms()
        mockupService.addGenomes()


    }
    def destroy = {
    }
}
