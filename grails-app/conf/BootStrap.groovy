import org.bbop.apollo.*

class BootStrap {

    def init = { servletContext ->
        if(User.count==0){
            def userRole = new Role(name: UserService.USER).save()
            userRole.addToPermissions("*:*")
            def adminRole = new Role(name: UserService.ADMIN).save()
            adminRole.addToPermissions("*:*")

            User demoUser = new User(username:"demo@demo.gov",passwordHash: "demo").save()
            demoUser.addToRoles(userRole)

            User adminUser = new User(username:"admin@admin.gov",passwordHash: "admin").save()
            adminUser.addToRoles(userRole)

        }

        if(Track.count==0){

        }

        if(Genome.count ==0){

        }

    }
    def destroy = {
    }
}
