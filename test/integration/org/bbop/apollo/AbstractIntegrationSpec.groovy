package org.bbop.apollo

import grails.test.spock.IntegrationSpec
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.crypto.hash.Sha256Hash
import org.apache.shiro.session.Session
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by nathandunn on 11/4/15.
 */
class AbstractIntegrationSpec extends IntegrationSpec{

    def shiroSecurityManager
    def projectionService

    def setup(){
       setupDefaultUserOrg()
//       projectionService.clearProjections()
    }

    String password = "testPass"
    String passwordHash = new Sha256Hash(password).toHex()

    String getTestCredentials(){
        "\"${FeatureStringEnum.CLIENT_TOKEN.value}\":\"1231232\",\"${FeatureStringEnum.USERNAME.value}\":\"test@test.com\","
    }

    // Logout the user fully before continuing.
    private void ensureUserIsLoggedOut()
    {
        try
        {
            // Get the user if one is logged in.
            Subject currentUser = SecurityUtils.getSubject();
            if (currentUser == null){
                return;
            }

            // Log the user out and kill their session if possible.
            currentUser.logout();
            Session session = currentUser.getSession(false);
            if (session == null){
                return;
            }

            session.stop();
        }
        catch (Exception e)
        {
            // Ignore all errors, as we're trying to silently
            // log the user out.
        }
    }

    def setupDefaultUserOrg(){
        ensureUserIsLoggedOut()

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
        Subject subject = SecurityUtils.getSubject();
        subject.login(authToken)

        Organism organism = new Organism(
                directory: "test/integration/resources/sequences/honeybee-Group1.10/"
                ,commonName: "sampleAnimal"
                ,genus: "Sample"
                ,species: "animal"
        ).save(failOnError: true)

        Sequence sequence = new Sequence(
                length: 1405242
                ,seqChunkSize: 20000
                ,start: 0
                ,end: 1405242
                ,organism: organism
                ,name: "Group1.10"
        ).save(failOnError: true)

        organism.addToSequences(sequence)
        organism.save(flush: true, failOnError: true)
    }

    JSONArray getCodingArray(JSONObject jsonObject) {
        JSONArray mrnaArray = jsonObject.getJSONArray(FeatureStringEnum.FEATURES.value)
        assert 1 == mrnaArray.size()
        return mrnaArray.getJSONObject(0).getJSONArray(FeatureStringEnum.CHILDREN.value)
    }
}
