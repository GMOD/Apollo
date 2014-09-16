import org.bbop.apollo.User

class BootStrap {

    def mockupService

    def init = { servletContext ->
//        if(User.count==0){
//
//        }



        if(User.count==0){
            mockupService.addUsers()
            mockupService.addTerms()
            mockupService.addGenomes()


        }

    }
    def destroy = {
    }
}
