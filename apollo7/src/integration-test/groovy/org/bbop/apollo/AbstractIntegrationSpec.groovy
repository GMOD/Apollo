package org.bbop.apollo

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.security.ApolloSecurityUtils
import org.bbop.apollo.security.Sha256PasswordEncoder
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import spock.lang.Specification

@Integration
@Rollback
class AbstractIntegrationSpec extends Specification {

    String password = "testPass"
    String passwordHash = new Sha256PasswordEncoder().encode(password)

    def setup() {
    }

    def cleanup() {
        ApolloSecurityUtils.logout()
    }

    def setupTestData() {
        setupDefaultUserOrg()
    }

    String getTestCredentials(String clientToken = "1231232") {
        "\"${FeatureStringEnum.CLIENT_TOKEN.value}\":\"${clientToken}\",\"${FeatureStringEnum.USERNAME.value}\":\"test@test.com\","
    }

    def setupDefaultUserOrg() {
        if (User.findByUsername('test@test.com')) {
            ApolloSecurityUtils.loginUser('test@test.com', [GlobalPermissionEnum.ADMIN.name()])
            return
        }

        User testUser = new User(
                username: 'test@test.com'
                , firstName: 'Bob'
                , lastName: 'Test'
                , passwordHash: passwordHash
        ).save(insert: true, flush: true)
        def adminRole = Role.findByName(GlobalPermissionEnum.ADMIN.name())
        if (!adminRole) {
            adminRole = new Role(name: GlobalPermissionEnum.ADMIN.name()).save(flush: true)
        }
        testUser.addToRoles(adminRole)
        testUser.save(flush: true)

        ApolloSecurityUtils.loginUser(testUser.username, [GlobalPermissionEnum.ADMIN.name()])

        Organism organism = new Organism(
                directory: "src/integration-test/resources/sequences/honeybee-Group1.10/"
                , commonName: "sampleAnimal"
                , genus: "Sample"
                , species: "animal"
        ).save(failOnError: true)

        Sequence sequence = new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , end: 1405242
                , organism: organism
                , name: "Group1.10"
        ).save(failOnError: true)

        organism.addToSequences(sequence)
        organism.save(flush: true, failOnError: true)

        new UserOrganismPreference(
                user: testUser
                , organism: organism
                , currentOrganism: true
                , sequence: sequence
                , clientToken: "1231232"
        ).save(flush: true, failOnError: true)
    }

    JSONArray getCodingArray(JSONObject jsonObject) {
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == mrnaArray.size()
        return mrnaArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
    }
}
