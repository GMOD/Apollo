#!/usr/bin/env groovy
import groovyjarjarcommonscli.Option
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')

String usageString = "\nUSAGE: alter_user_groups_and_permissions.groovy <options>\n" +
        "Example:\n" +
        "./alter_user_groups_and_permissions.groovy -inputfile user_to_permissions.csv -destinationurl http://localhost:8080/Apollo/\n" +
        "./alter_user_groups_and_permissions.groovy -user test@admin.gov -organism organism_name -permission READ:WRITE -destinationurl http://localhost:8080/Apollo/\n" +
        "./alter_user_groups_and_permissions.groovy -user test@admin.gov -addToGroup group1 -removeFromGroup group2:group3 -destinationurl http://localhost:8080/Apollo/\n" +
        "./alter_user_groups_and_permissions.groovy -user test@admin.gov -organism organism_name -permission READ:WRITE -addToGroup group1 -removeFromGroup group2 -destinationurl http://localhost:8080/Apollo/"

def cli = new CliBuilder(usage: 'alter_user_groups_and_permissions.groovy')
cli.setStopAtNonOption(true)
cli.inputfile('CSV file with format <username/email>,<organism>,<permissions>,<groups to add user to>,<groups to remove user from>', required: false, args: 1)
cli.user('email/username for a user', required: false, args: 1)
cli.organism('Organism for which permissions have to be altered for username', required: false, args: 1)
cli.permission('Permission(s) to be granted for username on organism, separated by \':\'.  They can be READ, WRITE, EXPORT, ADMINISTRATE', required: false, args: 1)
cli.addToGroup('Add user to group(s), separated by \':\'', required: false, args: 1)
cli.removeFromGroup('Remove user from group(s), separated by \':\'', required: false, args: 1)
cli.destinationurl('Apollo URL', required: true, args: 1)
cli.adminusername('Admin username', required: false, args: 1)
cli.adminpassword('Admin password', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options

def admin_username
def admin_password
def userPermissionMap = [:]

try {
    options = cli.parse(args)
    if (!(options?.destinationurl)) {
        println "NOTE: Requires destination URL\n" + usageString
        return
    }
    if (!options?.inputfile && !options?.user) {
        println "NOTE: Requires a CSV as inputfile\nOR\n user, organism and permission as arguments\nOR\n user, addToGroup, removeFromGroup\n" + usageString
        return
    }
    if (options?.inputfile && options?.user) {
        println "NOTE: Requires a CSV as inputfile\nOR\n user, organism and permission as arguments\nOR\n user, addToGroup, removeFromGroup\n" + usageString
        return
    }
    if (options?.permission && !options?.organism) {
        println "NOTE: Need organism as argument for updating permissions\n" + usageString
        return
    }
    def sysConsole = System.console()
    if (!(admin_username=options?.adminusername)) {
        admin_username = new String(sysConsole.readLine('Enter admin username: '))
    }
    if (!(admin_password=options?.adminpassword)) {
        admin_password = new String(sysConsole.readPassword('Enter admin password: '))
    }
    
} catch(e) {
    println(e)
    return
}

if (options.inputfile) {
    userPermissionMap = parseInputFile(options.inputfile)
}
else {
    JSONObject userObject = new JSONObject()
    userObject.organism = options.organism
    userObject.ADMINISTRATE = false
    userObject.EXPORT = false
    userObject.READ = false
    userObject.WRITE = false
    userObject.addtogroup = options?.addToGroup ? options.addToGroup.split(':') : []
    userObject.removefromgroup = options?.removeFromGroup ? options.removeFromGroup.split(':') : []
    def permissionArray = options?.permission ? options.permission.split(':') : []
    permissionArray.each {
        if (it == 'ADMINISTRATE') {userObject.ADMINISTRATE = true}
        else if (it == 'EXPORT') {userObject.EXPORT = true}
        else if (it == 'READ') {userObject.READ = true}
        else if (it == 'WRITE') {userObject.WRITE = true}
    }
    userPermissionMap.put(options.user, userObject)
}

def s=options.destinationurl
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1)
}

URL url = new URL(s)
def client = new RESTClient(options.destinationurl)
if (options.ignoressl) { client.ignoreSSLIssues() }
String updateOrganismPermissionPath = "${url.path}/user/updateOrganismPermission"
String addUserToGroupPath = "${url.path}/user/addUserToGroup"
String removeUserFromGroupPath = "${url.path}/user/removeUserFromGroup"

for (user in userPermissionMap.keySet()) {
    println "Processing user: ${user}"
    JSONObject userObject = userPermissionMap.get(user) as JSONObject
    if ((options?.permission && !options?.inputfile) || (!options?.permission && options?.inputfile)) {
        updateOrganismPermission(user, userObject, updateOrganismPermissionPath, client, admin_username, admin_password)
    }
    if ((options?.addToGroup && !options?.inputfile) || (!options?.addToGroup && options?.inputfile)) {
        addUserToGroup(user, userObject, addUserToGroupPath, client, admin_username, admin_password)
    }
    if ((options?.removeFromGroup && !options?.inputfile) || (!options?.removeFromGroup && options?.inputfile)) {
        removeUserFromGroup(user, userObject, removeUserFromGroupPath, client, admin_username, admin_password)
    }
}

def updateOrganismPermission(String user, JSONObject userObject, String path, RESTClient client, String username, String password) {
    def userArgument = [
            user: user,
            organism: userObject.organism,
            ADMINISTRATE: userObject.ADMINISTRATE,
            EXPORT: userObject.EXPORT,
            READ: userObject.READ,
            WRITE: userObject.WRITE,
            username: username,
            password: password
    ]
    def response = client.post(
            contentType: 'text/javascript',
            path: path,
            body: userArgument
    )
    
    if (response.data.error) {
        println "Error while altering permissions for user: ${user}\n${response.data.error}"
    }
    assert response.status == 200
}

def addUserToGroup(String user, JSONObject userObject, String path, RESTClient client, String username, String password) {
    for (String group : userObject.addtogroup) {
        if (group == '' || group == null) {continue}
        def userArgument = [
                user: user,
                group: group,
                username: username,
                password: password
        ]

        def response = client.post(
                contentType: 'text/javascript',
                path: path,
                body: userArgument
        )
        
        if (response.data.error) {
            println "Error while adding user ${user.user} to group ${group}\n${response.data.error}"
        }
        assert response.status == 200
    }
}

def removeUserFromGroup(String user, JSONObject userObject, String path, RESTClient client, String username, String password) {
    for (String group : userObject.removefromgroup) {
        if (group == '' || group == null) {continue}
        def userArgument = [
                user: user,
                group: group,
                username: username,
                password: password
        ]

        def response = client.post(
                contentType: 'text/javascript',
                path: path,
                body: userArgument
        )

        if (response.data.error) {
            println "Error while removing user ${user.user} from group ${group}\n${response.data.error}"
        }
        assert response.status == 200
    }
}

def parseInputFile(String inputFile) {
    def permissionMap = [:]
    new File(inputFile).splitEachLine(',') { fields ->
        if (fields.size() != 5) {
            println "ERROR: Improper formatting in ${inputFile} at line:\n${fields.join(',')}"
            return
        }
        JSONObject userPermissionObject = new JSONObject()
        userPermissionObject.ADMINISTRATE = false
        userPermissionObject.EXPORT = false
        userPermissionObject.READ = false
        userPermissionObject.WRITE = false
        String userName = fields[0]
        if (permissionMap.containsKey(userName)) {
            println "Duplicate entries for user: ${userName}"
        }
        else {
            permissionMap.put(userName, userPermissionObject)
        }
        String organism = fields[1]
        def permissionArray = fields[2].split(':')
        permissionArray.each { 
            if (it == 'ADMINISTRATE') {
                permissionMap.get(userName).ADMINISTRATE = true
            }
            else if (it == 'EXPORT') {
                permissionMap.get(userName).EXPORT = true
            }
            else if (it == 'READ') {
                permissionMap.get(userName).READ = true
            }
            else if (it == 'WRITE') {
                permissionMap.get(userName).WRITE = true
            }
            else {
                println "Unrecognized permission found for user: ${userName}"
                System.exit(1)
            }
        }
        permissionMap.get(userName).organism = organism
        permissionMap.get(userName).addtogroup = fields[3].split(':')
        permissionMap.get(userName).removefromgroup = fields[4].split(':')
    }
    return permissionMap
}