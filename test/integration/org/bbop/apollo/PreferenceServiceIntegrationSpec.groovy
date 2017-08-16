package org.bbop.apollo

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager

class PreferenceServiceIntegrationSpec extends AbstractIntegrationSpec {


    def preferenceService


    def setupDefaultUserOrg(){

        if(User.findByUsername('test@test.com')){
            return
        }

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
        Subject subject = SecurityUtils.getSubject()
        subject.login(authToken)

        Organism organism1 = new Organism(
                directory: "test/integration/resources/sequences/honeybee-tracks/"
                ,commonName: "sampleAnimal"
                ,genus: "Sample"
                ,species: "animal"
        ).save(failOnError: true)

        Sequence sequence1Org1 = new Sequence(
                length: 1405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                ,organism: organism1
                ,name: "Group1.10"
        ).save(failOnError: true)

        Sequence sequence2Org1 = new Sequence(
                length: 1405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                ,organism: organism1
                ,name: "GroupUn87"
        ).save(failOnError: true)


        organism1.addToSequences(sequence1Org1)
        organism1.addToSequences(sequence2Org1)
        organism1.save(flush: true, failOnError: true)

        Organism organism2 = new Organism(
                directory: "test/integration/resources/sequences/yeast/"
                ,commonName: "yeast"
                ,genus: "Sample"
                ,species: "animal"
        ).save(failOnError: true)

        Sequence sequence1Org2 = new Sequence(
                length: 2405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 2405242
                ,organism: organism2
                ,name: "chrI"
        ).save(failOnError: true)

        Sequence sequence2Org2 = new Sequence(
                length: 2405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 2405242
                ,organism: organism2
                ,name: "chrII"
        ).save(failOnError: true)

        organism2.addToSequences(sequence1Org2)
        organism2.addToSequences(sequence2Org2)
        organism2.save(flush: true, failOnError: true)



    }

    void "change sequences between one organism"() {

    }

    void "change organisms"() {

    }

    void "change sequences between organisms and verify they are the same when returning"(){

    }
}
