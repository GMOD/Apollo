import org.apache.shiro.crypto.hash.Sha256Hash
import org.bbop.apollo.*

class BootStrap {

    def init = { servletContext ->
//        if(User.count==0){
//
//        }


        if(Track.count==0){
            def userRole = new Role(name: UserService.USER).save()
            userRole.addToPermissions("*:*")
            def adminRole = new Role(name: UserService.ADMIN).save()
            adminRole.addToPermissions("*:*")

            User demoUser = new User(username:"demo@demo.gov"
                    ,passwordHash: new Sha256Hash("demo").toHex()
            ).save()
            demoUser.addToRoles(userRole)

            User adminUser = new User(username:"admin@admin.gov"
                    ,passwordHash: new Sha256Hash("admin").toHex()
            ).save()
            adminUser.addToRoles(userRole)


            Genome genome1 = new Genome(name: "Danio rerio").save()
            Track track1 = new Track(name: "Zebrafish Track 1").save()
            Track track2 = new Track(name: "Zebrafish Track 2").save()
            genome1.addToTracks(track1)
            genome1.addToTracks(track2)
            track1.addToUsers(demoUser)
            track1.addToUsers(adminUser)

            Genome genome2 = new Genome(name: "Caenorhabditis elegans").save()
            Track track3 = new Track(name: "Celegans Track 1").save()
            genome2.addToTracks(track3)
            track3.addToUsers(demoUser)

            track3.save(flush:true)
        }

    }
    def destroy = {
    }
}
