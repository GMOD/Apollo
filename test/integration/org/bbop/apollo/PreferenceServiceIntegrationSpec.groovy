package org.bbop.apollo

import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
import org.codehaus.groovy.grails.web.json.JSONObject
import spock.lang.IgnoreRest

class PreferenceServiceIntegrationSpec extends AbstractIntegrationSpec {


    def preferenceService
    def annotatorService
    def requestHandlingService


    def setupDefaultUserOrg() {

        if (User.findByUsername('test@test.com')) {
            return
        }

        User testUser = new User(
                username: 'test@test.com'
                , firstName: 'Bob'
                , lastName: 'Test'
                , passwordHash: passwordHash
        ).save(insert: true, flush: true)
        def adminRole = Role.findByName(UserService.ADMIN)
        testUser.addToRoles(adminRole)
        testUser.save()

        shiroSecurityManager.sessionManager = new DefaultWebSessionManager()
        ThreadContext.bind(shiroSecurityManager)
        def authToken = new UsernamePasswordToken(testUser.username, password as String)
        Subject subject = SecurityUtils.getSubject()
        subject.login(authToken)

        Organism organism1 = new Organism(
                directory: "test/integration/resources/sequences/honeybee-tracks/"
                , commonName: "honeybee"
                , genus: "Honey"
                , species: "bee"
        ).save(failOnError: true)

        Sequence sequence1Org1 = new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , end: 1405242
                , organism: organism1
                , name: "Group1.10"
        ).save(failOnError: true)

        Sequence sequence2Org1 = new Sequence(
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism1
                , name: "GroupUn87"
        ).save(failOnError: true)


        organism1.addToSequences(sequence1Org1)
        organism1.addToSequences(sequence2Org1)
        organism1.save(flush: true, failOnError: true)

        Organism organism2 = new Organism(
                directory: "test/integration/resources/sequences/yeast/"
                , commonName: "yeast"
                , genus: "Sample"
                , species: "animal"
        ).save(failOnError: true)

        Sequence sequence1Org2 = new Sequence(
                length: 230208
                , seqChunkSize: 20000
                , start: 0
                , end: 230208
                , organism: organism2
                , name: "chrI"
        ).save(failOnError: true)

        Sequence sequence2Org2 = new Sequence(
                length: 813178
                , seqChunkSize: 20000
                , start: 0
                , end: 813178
                , organism: organism2
                , name: "chrII"
        ).save(failOnError: true)

        organism2.addToSequences(sequence1Org2)
        organism2.addToSequences(sequence2Org2)
        organism2.save(flush: true, failOnError: true)


    }

    void "change organisms"() {

        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to organism 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end



        when: "we set some location data and flush the preference saved on organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism2.name, 100, 200, token)

        then: "we verify that it is saved on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we change organisms back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "we set the location on organism 1 flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 300, 400, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we go back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that the location / sequence is as we set it for organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we go back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that the location / sequence is as we set it for organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


    }

    void "change sequences between one organism"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1, sequence 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to sequence 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence2Organism1, token)
        preferenceService.evaluateSaves(true)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence2Organism1.end

        when: "we set some location data and flush the preference saved on sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence2Organism1.name, 300, 400, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that it is saved on organism 1, sequence 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we change organisms back to sequence 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence1Organism1, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "we set the location on sequence 1 and flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 100, 200, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we go back to sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence2Organism1, token)
        preferenceService.evaluateSaves(true)

        then: "we verify that the location / sequence is as we set it for sequence 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we go back to sequence 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence1Organism1, token)
        preferenceService.evaluateSaves(true)

        then: "we verify that the location / sequence is as we set it for sequence 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200

    }


    void "change sequences between organisms and verify they are the same when returning"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1, sequence 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to sequence 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence2Organism1, token)
        preferenceService.evaluateSaves(true)

        then: "verify some other things on organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence2Organism1.end

        when: "we set some location data and flush the preference saved on sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence2Organism1.name, 300, 400, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that it is saved on organism 1, sequence 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

        when: "we change organisms back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)


        then: "we verify that it has been moved to 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end


        when: "we set the location on organism 2 flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism2.name, 100,200, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we go back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that the location / sequence is as we set it for organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

        when: "we go back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)
        preferenceService.evaluateSaves(true)


        then: "we verify that the location / sequence is as we set it for organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200
    }


    void "repeat changing organism without flushing to DB"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to organism 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end



        when: "we set some location data and flush the preference saved on organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism2.name, 100, 200, token)

        then: "we verify that it is saved on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we change organisms back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "we set the location on organism 1 flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 300, 400, token)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we go back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)


        then: "we verify that the location / sequence is as we set it for organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we go back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that the location / sequence is as we set it for organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

    }

    void "repeat changing sequence without flushing to DB"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1, sequence 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to sequence 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence2Organism1, token)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence2Organism1.end

        when: "we set some location data and flush the preference saved on sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence2Organism1.name, 300, 400, token)


        then: "we verify that it is saved on organism 1, sequence 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we change organisms back to sequence 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence1Organism1, token)


        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "we set the location on sequence 1 and flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 100, 200, token)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we go back to sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence2Organism1, token)

        then: "we verify that the location / sequence is as we set it for sequence 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we go back to sequence 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence1Organism1, token)

        then: "we verify that the location / sequence is as we set it for sequence 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200
    }

    void "repeat changing organism and sequence without flushing to DB"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to organism 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end



        when: "we set some location data and flush the preference saved on organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism2.name, 100, 200, token)

        then: "we verify that it is saved on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we change organisms back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "we set the location on organism 1 flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 300, 400, token)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400


        when: "we go back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)


        then: "we verify that the location / sequence is as we set it for organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we go back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that the location / sequence is as we set it for organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

    }

    void "changing organism and add a feature"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we add a transcript for organism 1"
        String featureString2 = "{${getTestCredentials(token)} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":974306,\"fmax\":975778,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40733-RA\",\"children\":[{\"location\":{\"fmin\":974306,\"fmax\":975778,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        requestHandlingService.addTranscript(JSON.parse(featureString2) as JSONObject)

        then: "we expect to see it"
        assert Gene.count==1
        assert MRNA.count==1


        when: "we switch to organism 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end



        when: "we add a feature on organism 2"
        String featureString = "{${getTestCredentials(token)} \"track\":\"chrI\",\"features\":[{\"location\":{\"fmin\":114919,\"fmax\":118315,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"YAL019W\",\"children\":[{\"location\":{\"fmin\":114919,\"fmax\":118315,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        requestHandlingService.addTranscript(JSON.parse(featureString) as JSONObject)

        then: "we verify it made it"
        assert Gene.count==2
        assert MRNA.count==2

        when: "we change organisms back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)

        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "when we add a feature onto organism 1"
        featureString2 = "{${getTestCredentials(token)} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":974306,\"fmax\":975778,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40733-RA\",\"children\":[{\"location\":{\"fmin\":974306,\"fmax\":975778,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        requestHandlingService.addTranscript(JSON.parse(featureString2) as JSONObject)


        then: "we verify that we added one here"
        assert Gene.count==2
        assert MRNA.count==3
    }

    void "repeat changing organism and sequence and add features along the way"() {
        given: "setting up two organisms and sequences"
        String token = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject = annotatorService.getAppState(token)

        then: "verify some stuff on organism 1"
        assert appStateObject.currentOrganism.commonName == organism1.commonName
        assert appStateObject.currentSequence.name == sequence1Organism1.name

        when: "we switch to organism 2"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)

        then: "verify some other things on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end



        when: "we set some location data and flush the preference saved on organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism2.name, 100, 200, token)

        then: "we verify that it is saved on organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we change organisms back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that it has been moved back 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end


        when: "we set the location on organism 1 flush preference"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 300, 400, token)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

        when: "we add a transcript for organism 1"
        String featureString2 = "{${getTestCredentials(token)} \"track\":\"Group1.10\",\"features\":[{\"location\":{\"fmin\":974306,\"fmax\":975778,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"GB40733-RA\",\"children\":[{\"location\":{\"fmin\":974306,\"fmax\":975778,\"strand\":-1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        requestHandlingService.addTranscript(JSON.parse(featureString2) as JSONObject)

        then: "we expect to see it"
        assert Gene.count==1
        assert MRNA.count==1

        when: "we go back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token)


        then: "we verify that the location / sequence is as we set it for organism 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200

        when: "we add a feature on organism 2"
        String featureString = "{${getTestCredentials(token)} \"track\":\"chrII\",\"features\":[{\"location\":{\"fmin\":114919,\"fmax\":118315,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"mRNA\"},\"name\":\"YAL019W\",\"children\":[{\"location\":{\"fmin\":114919,\"fmax\":118315,\"strand\":1},\"type\":{\"cv\":{\"name\":\"sequence\"},\"name\":\"exon\"}}]}],\"operation\":\"add_transcript\"}"
        requestHandlingService.addTranscript(JSON.parse(featureString) as JSONObject)

        then: "we verify it made it"
        assert Gene.count==2
        assert MRNA.count==2


        when: "we go back to organism 1"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token)


        then: "we verify that the location / sequence is as we set it for organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

        when: "we add a transcript for organism 1"
        requestHandlingService.addTranscript(JSON.parse(featureString2) as JSONObject)

        then: "we expect to see it"
        assert Gene.count==2
        assert MRNA.count==3

    }


    @Ignore
    void "change sequences between organisms and different tokens should remain independent"() {
        given: "setting up two organisms and sequences"
        String token1 = ClientTokenGenerator.generateRandomString()
        String token2 = ClientTokenGenerator.generateRandomString()
        Organism organism1 = Organism.findByCommonName("honeybee") // honeybee
        Sequence sequence1Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.first() // Group1.10
        Sequence sequence2Organism1 = organism1.sequences.sort(){a,b -> a.name <=> b.name }.last()  // GroupUn87
        Organism organism2 = Organism.findByCommonName("yeast")
        Sequence sequence1Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.first() // ChrI
        Sequence sequence2Organism2 = organism2.sequences.sort(){a,b -> a.name <=> b.name }.last()  // ChrII
        User user = User.first()

        when: "we setup the first two"
        JSONObject appStateObject1 = annotatorService.getAppState(token1)
        JSONObject appStateObject2 = annotatorService.getAppState(token2)

        then: "verify some stuff on organism 1, sequence 1"
        assert appStateObject1.currentOrganism.commonName == organism1.commonName
        assert appStateObject1.currentSequence.name == sequence1Organism1.name
        assert appStateObject2.currentOrganism.commonName == organism1.commonName
        assert appStateObject2.currentSequence.name == sequence1Organism1.name


        when: "we set the location on organism 1 flush preference"
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence1Organism1.name, 100,200, token2)


        then: "we verify that it has been flushed"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism1.name
        assert userOrganismPreferenceDTO.startbp == 100
        assert userOrganismPreferenceDTO.endbp == 200


        when: "we switch to sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequence(user, sequence2Organism1, token1)

        then: "verify some other things on organism 1"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence2Organism1.end

        when: "we set some location data and flush the preference saved on sequence 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentSequenceLocation(sequence2Organism1.name, 300, 400, token1)


        then: "we verify that it is saved on organism 2, sequence 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 300
        assert userOrganismPreferenceDTO.endbp == 400

        when: "we change organisms back to organism 2"
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token2)


        then: "we verify that it has been moved to 2"
        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism2.end



        when: "we go back to organism 1"
        def allPRefs = UserOrganismPreference.all
        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism1, token2)


        then: "we verify that the location / sequence is as we set it for organism 1 (not from the previous setting"
        assert userOrganismPreferenceDTO.organism.commonName == organism1.commonName
        assert userOrganismPreferenceDTO.sequence.name == sequence2Organism1.name
        assert userOrganismPreferenceDTO.startbp == 0
        assert userOrganismPreferenceDTO.endbp == sequence1Organism1.end

//        when: "we go back to organism 2"
//        userOrganismPreferenceDTO = preferenceService.setCurrentOrganism(user, organism2, token1)
//
//
//        then: "we verify that the location / sequence is as we set it for organism 2"
//        assert userOrganismPreferenceDTO.organism.commonName == organism2.commonName
//        assert userOrganismPreferenceDTO.sequence.name == sequence1Organism2.name
//        assert userOrganismPreferenceDTO.startbp == 100
//        assert userOrganismPreferenceDTO.endbp == 200
    }
}
