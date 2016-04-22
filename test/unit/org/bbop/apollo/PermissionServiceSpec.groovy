package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.bbop.apollo.gwt.shared.PermissionEnum
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
                commonName: "Honeybee"
                ,directory:  "test/integration/resources/sequences/honeybee-Group1.10/"
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
        service.setOrganismPermissionsForUser(permissionEnumList2,organism,user,"123123")
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
        service.setOrganismPermissionsForUserGroup(permissionEnumList1,organism,group,"123123")
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
