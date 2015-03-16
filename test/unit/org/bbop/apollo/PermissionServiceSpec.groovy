package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(PermissionService)
@Mock([User,Organism,UserGroup,GroupOrganismPermission,UserOrganismPermission,UserTrackPermission,UserOrganismPermission,GroupTrackPermission])
class PermissionServiceSpec extends Specification {

    def setup() {
        
        User bobUser = new User(
                username: 'bob@bob.com'
                ,firstName: "Bob"
                ,lastName: "Jones"
                ,passwordHash: "asdfasdf"
        ).save()
        User user2 = new User(
                username: 'test@test.com'
                ,firstName: "Test"
                ,lastName: "Case"
                ,passwordHash: "asdfasdf"
        ).save()

        UserGroup userGroup = new UserGroup(
                name: 'WorkGroup'
        ).save()
        userGroup.addToUsers(bobUser)
        bobUser.addToUserGroups(userGroup)

        userGroup.addToUsers(user2)
        user2.addToUserGroups(userGroup)
        
        Organism organism = new Organism(
                commonName: "Perch"
                ,directory: "asdf"
        ).save()
    }

    def cleanup() {
    }

    void "merge organism permissions"() {
        
        given: "a list of permissions"
        List<PermissionEnum> permissionEnums1 = new ArrayList<>()
        List<PermissionEnum> permissionEnums2 = new ArrayList<>()
        List<PermissionEnum> permissionEnums3 = new ArrayList<>()
        
        when: "we populate them"
        permissionEnums1.add(PermissionEnum.READ)
        permissionEnums1.add(PermissionEnum.ADMINISTRATE)
        permissionEnums1.add(PermissionEnum.WRITE)
        permissionEnums2.add(PermissionEnum.READ)
        permissionEnums2.add(PermissionEnum.EXPORT)
        permissionEnums3.add(PermissionEnum.READ)
        
        then: "we should get the right stuff back"
        assert permissionEnums1.size()==3
        assert permissionEnums2.size()==2
        assert permissionEnums3.size()==1

        when: "we merge 1 and 2"
        Collection<PermissionEnum> combo1 = service.mergeOrganismPermissions(permissionEnums1,permissionEnums2)
        
        
        then: "we should merged appropriate"
        assert combo1.size()==4
        assert combo1.contains(PermissionEnum.READ)
        assert combo1.contains(PermissionEnum.ADMINISTRATE)
        assert combo1.contains(PermissionEnum.EXPORT)
        assert combo1.contains(PermissionEnum.WRITE)

        when: "we add the simple 1"
        Collection<PermissionEnum> combo2 = service.mergeOrganismPermissions(permissionEnums2,permissionEnums3)
        
        then: "should merge properly"
        assert combo2.size()==2
        assert combo2.contains(PermissionEnum.READ)
        assert combo2.contains(PermissionEnum.EXPORT)
    }
    
    void "merge track visibility"(){
       
        given: "2 sets of track visibilities"
        Map<String,Boolean> map1 = new HashMap<>()
        Map<String,Boolean> map2 = new HashMap<>()
        
        when: "we populate the maps"
        map1.put("trackA",true)
        map1.put("trackB",false)
        map1.put("trackC",false)
        map2.put("trackA",false)
        map2.put("trackC",true)
        map2.put("trackD",true)
        Map<String,Boolean> combo1 = service.mergeTrackVisibilityMaps(map1,map2)
        
        then: "combined maps are"
        assert 4==combo1.size()
        assert combo1.get("trackA")
        assert !combo1.get("trackB")
        assert combo1.get("trackC")
        assert combo1.get("trackD")
    }
    
    void "test user track permissions "(){
        
        given: "a user, organism, and group"
        User user = User.first()
        Organism organism = Organism.first()
        UserGroup group = UserGroup.first()
        
        when: "we assign a user visibility to a track"
        Map<String,Boolean> visibilityMap = new HashMap<>()
        visibilityMap.put("trackA",true)
        visibilityMap.put("trackB",false)
        service.setTracksVisibleForOrganismAndUser(visibilityMap,organism,user)
        Map<String,Boolean> trackVisibility = service.getTracksVisibleForOrganismAndUser(organism,user)
        
        
        then: "they should be able to see that track and not others"
        assert trackVisibility.size()==2
        assert trackVisibility.get("trackA")
        assert !trackVisibility.get("trackB")
        
        when: "we add some group permissions, should always tack the most"
        Map<String,Boolean> visibilityMap2 = new HashMap<>()
//        visibilityMap2.put("trackA",false)
        visibilityMap2.put("trackB",true)
        visibilityMap2.put("trackC",false)
        service.setTracksVisibleForOrganismAndGroup(visibilityMap2,organism,group)
        
        trackVisibility = service.getTracksVisibleForOrganismAndUser(organism,user)
        Map<String,Boolean> groupTrackVisibility = service.getTracksVisibleForOrganismAndGroup(organism,group)

        then: "we should see identical group permissions"
        assert 2==groupTrackVisibility.size()
        assert groupTrackVisibility.get("trackB")
        assert !groupTrackVisibility.get("trackC")
        assert 3==trackVisibility.size()
        assert trackVisibility.get("trackA")
        assert trackVisibility.get("trackB")
        assert !trackVisibility.get("trackC")


        when: "we update our group track permission"
        Map<String,Boolean> visibilityMap3 = new HashMap<>()
        visibilityMap3.put("trackA",false)
        visibilityMap3.put("trackB",false)
        visibilityMap3.put("trackC",false)
        visibilityMap3.put("trackD",true)
        service.setTracksVisibleForOrganismAndGroup(visibilityMap3,organism,group)
        groupTrackVisibility = service.getTracksVisibleForOrganismAndGroup(organism,group)
        trackVisibility = service.getTracksVisibleForOrganismAndUser(organism,user)

        then: "then we see that there is another group track"
        assert 4==groupTrackVisibility.size()
        assert !groupTrackVisibility.get("trackA")
        assert !groupTrackVisibility.get("trackB")
        assert !groupTrackVisibility.get("trackC")
        assert groupTrackVisibility.get("trackD")

        assert 4==trackVisibility.size()
        assert trackVisibility.get("trackA")
        assert !trackVisibility.get("trackB")
        assert !trackVisibility.get("trackC")
        assert trackVisibility.get("trackD")

    }
    
    void "set organism permissions "(){

        given: "a user, organism, and group"
        User user = User.first()
        Organism organism = Organism.first()
        UserGroup group = UserGroup.first()


        when: "we add permissions to a user"
        List<PermissionEnum> permissionEnumList2 = new ArrayList<>()
        permissionEnumList2.add(PermissionEnum.ADMINISTRATE)
        permissionEnumList2.add(PermissionEnum.READ)
        permissionEnumList2.add(PermissionEnum.WRITE)
        service.setOrganismPermissionsForUser(permissionEnumList2,organism,user)
        List<PermissionEnum> userPermissionEnumsReceived2 = service.getOrganismPermissionsForUser(organism,user)

        then: "we should see the same come back "
        assert 3==userPermissionEnumsReceived2.size()
        assert userPermissionEnumsReceived2.contains(PermissionEnum.ADMINISTRATE)
        assert userPermissionEnumsReceived2.contains(PermissionEnum.WRITE)
        assert userPermissionEnumsReceived2.contains(PermissionEnum.READ)


        when: "we add permission to a group"
        List<PermissionEnum> permissionEnumList1 = new ArrayList<>()
        permissionEnumList1.add(PermissionEnum.READ)
        permissionEnumList1.add(PermissionEnum.EXPORT)
        service.setOrganismPermissionsForUserGroup(permissionEnumList1,organism,group)
        List<PermissionEnum> userPermissionEnumsReceived1 = service.getOrganismPermissionsForUserGroup(organism,group)
        userPermissionEnumsReceived2 = service.getOrganismPermissionsForUser(organism,user)
        List<PermissionEnum> userPermissionEnumsReceived3 = service.getOrganismPermissionsForUser(organism,User.all.get(1))

        then: "we should get back the same for group, and combined for user"
        assert 2==userPermissionEnumsReceived1.size()
        assert userPermissionEnumsReceived1.contains(PermissionEnum.READ)
        assert userPermissionEnumsReceived1.contains(PermissionEnum.EXPORT)

        assert 4==userPermissionEnumsReceived2.size()
        assert userPermissionEnumsReceived2.contains(PermissionEnum.ADMINISTRATE)
        assert userPermissionEnumsReceived2.contains(PermissionEnum.WRITE)
        assert userPermissionEnumsReceived2.contains(PermissionEnum.READ)
        assert userPermissionEnumsReceived2.contains(PermissionEnum.EXPORT)

        assert 2==userPermissionEnumsReceived3.size()
        assert userPermissionEnumsReceived3.contains(PermissionEnum.READ)
        assert userPermissionEnumsReceived3.contains(PermissionEnum.EXPORT)

        

    }
}
