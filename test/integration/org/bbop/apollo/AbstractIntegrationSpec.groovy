package org.bbop.apollo

import grails.test.spock.IntegrationSpec
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.crypto.hash.Sha256Hash
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager

/**
 * Created by nathandunn on 11/4/15.
 */
class AbstractIntegrationSpec extends IntegrationSpec{

    def shiroSecurityManager

    String password = "testPass"
    String passwordHash = new Sha256Hash(password).toHex()

    def setupDefaultUserOrg(){

        User testUser = new User(
                username: 'test@test.com'
                ,firstName: 'Bob'
                ,lastName: 'Test'
                ,passwordHash: passwordHash
        ).save(insert: true,flush: true)
        def adminRole = Role.findByName(UserService.ADMIN)
        testUser.addToRoles(adminRole)
        testUser.save()

        shiroSecurityManager.sessionManager = new DefaultWebSessionManager()
        ThreadContext.bind(shiroSecurityManager)
        def authToken = new UsernamePasswordToken(testUser.username,password as String)
        Subject subject = SecurityUtils.getSubject();
        subject.login(authToken)

        Organism organism = new Organism(
                directory: "test/integration/resources/sequences/honeybee-Group1.10/"
                ,commonName: "sampleAnimal"
        ).save(flush: true)

        Sequence sequence = new Sequence(
                length: 1405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                ,organism: organism
                ,name: "Group1.10"
        ).save()
    }

}
