package org.bbop.apollo.authenticator

import grails.gorm.transactions.Transactional
import org.bbop.apollo.Role
import org.bbop.apollo.User
import org.bbop.apollo.UserGroup
import org.bbop.apollo.UserService
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.security.ApolloSecurityUtils
import org.bbop.apollo.security.Sha256PasswordEncoder

import jakarta.servlet.http.HttpServletRequest

@Transactional
class RemoteUserAuthenticatorService implements AuthenticatorService {

    String defaultGroup

    static String INTERNAL_PASSWORD = "INTERNAL_PASSWORD"

    private static final Sha256PasswordEncoder passwordEncoder = new Sha256PasswordEncoder()

    def authenticate(HttpServletRequest request) {
        User user
        String username
        String randomPassword = ClientTokenGenerator.generateRandomString()
        String passwordHash = passwordEncoder.encode(randomPassword)
        try {
            String remoteUser = request.getHeader(FeatureStringEnum.REMOTE_USER.value)
            log.warn "Remote user found [${remoteUser}]"
            if (!remoteUser) {
                log.warn("No remote user passed in header!")
                return false
            }
            username = remoteUser
            user = User.findByUsername(username)
            log.warn "User exists ${user} ? "
            log.warn "for username: ${username}"
            if (!user) {

                log.warn "User does not exist so creating new user."

                user = new User(
                        username: remoteUser,
                        passwordHash: passwordHash,
                        firstName: "REMOTE_USER",
                        lastName: "${remoteUser}",
                        metadata: randomPassword
                )
                user.addMetaData(INTERNAL_PASSWORD, randomPassword)
                user.save(flush: true, failOnError: true, insert: true)

                Role role = Role.findByName(GlobalPermissionEnum.USER.name())
                log.debug "adding role: ${role}"
                user.addToRoles(role)
                role.addToUsers(user)

                if (this.defaultGroup) {
                    log.debug "adding user to default group: ${this.defaultGroup}"
                    UserGroup userGroup = UserGroup.findByName(this.defaultGroup)

                    userGroup = userGroup ?: new UserGroup(name: this.defaultGroup).save(flush: true)

                    user.addToUserGroups(userGroup)
                }

                role.save()
                user.save(flush: true)
                log.warn "User created ${user}"
            }

            // Verify password and login
            String internalPassword = user.getMetaData(INTERNAL_PASSWORD)
            if (!passwordEncoder.matches(internalPassword, user.passwordHash)) {
                // Force reset password
                log.error "Failed to authenticate user ${username}, resaving password and forcing"
                user.addMetaData(INTERNAL_PASSWORD, randomPassword)
                user.passwordHash = passwordHash
                log.warn("reset password and saving: " + user.getMetaData(INTERNAL_PASSWORD))
                user.save(flush: true, failOnError: true, insert: false)
            }

            ApolloSecurityUtils.loginUser(username, user.roles*.name)
            return true
        } catch (Exception ae) {
            log.error(ae.message, ae)
            return false
        }
    }

    def authenticate(String username, String password, HttpServletRequest request) {
        // token is ignored for remote user auth
        return authenticate(request)
    }

    def setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup
    }

    @Override
    Boolean requiresToken() {
        return false
    }
}
