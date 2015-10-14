#!/usr/bin/env groovy
import groovyjarjarcommonscli.Option
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')

String usageString = "\nUSAGE: alter_user_groups_and_permissions.groovy <options>\n" +
        "Example:\n" +
        "./alter_user_groups_and_permissions.groovy -inputfile user_to_permissions.csv -destinationurl http://localhost:8080/WebApollo2/\n" +
        "./alter_user_groups_and_permissions.groovy -userid 105 -organism organism_name -permission READ:WRITE -destinationurl http://localhost:8080/WebApollo2/\n" +
        "./alter_user_groups_and_permissions.groovy -userid 105 -addToGroup group1 -removeFromGroup group2:group3 -destinationurl http://localhost:8080/WebApollo2/\n" +
        "./alter_user_groups_and_permissions.groovy -userid 105 -organism organism_name -permission READ:WRITE -addToGroup group1 -removeFromGroup group2 -destinationurl http://localhost:8080/WebApollo2/"

def cli = new CliBuilder(usage: 'alter_user_groups_and_permissions.groovy')
cli.setStopAtNonOption(true)
cli.inputfile('CSV file with format <userid>,<organism>,<permissions>,<groups to add user to>,<groups to remove user from>', required: false, args: 1)
cli.userid('userId for a user', required: false, args: 1)
cli.organism('Organism for which permissions have to be altered for username', required: false, args: 1)
cli.permission('Permission(s) to be granted for username on organism, separated by \':\'', required: false, args: 1)
cli.addToGroup('Add user to group(s), separated by \':\'', required: false, args: 1)
cli.removeFromGroup('Remove user from group(s), separated by \':\'', required: false, args: 1)
cli.destinationurl('WebApollo URL', required: true, args: 1)
cli.adminusername('Admin username', required: false, args: 1)
cli.adminpassword('Admin password', required: false, args: 1)
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
    if (!options?.inputfile && !options?.userid) {
        println "NOTE: Requires a CSV as inputfile\nOR\n userid, organism and permission as arguments\nOR\n userid, addToGroup, removeFromGroup\n" + usageString
        return
    }
    if (options?.inputfile && options?.userid) {
        println "NOTE: Requires a CSV as inputfile\nOR\n userid, organism and permission as arguments\nOR\n userid, addToGroup, removeFromGroup\n" + usageString
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
    userPermissionMap.put(options.userid, userObject)
}

def s=options.destinationurl
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1)
}

URL url = new URL(s)
def client = new RESTClient(options.destinationurl)
String updateOrganismPermissionPath = "${url.path}/user/updateOrganismPermission"
String addUserToGroupPath = "${url.path}/user/addUserToGroup"
String removeUserFromGroupPath = "${url.path}/user/removeUserFromGroup"

for (userId in userPermissionMap.keySet()) {
    println "Processing userid: ${userId}"
    JSONObject userIdObject = userPermissionMap.get(userId) as JSONObject
    if ((options?.permission && !options?.inputfile) || (!options?.permission && options?.inputfile)) {
        updateOrganismPermission(userId, userIdObject, updateOrganismPermissionPath, client, admin_username, admin_password)
    }
    if ((options?.addToGroup && !options?.inputfile) || (!options?.addToGroup && options?.inputfile)) {
        addUserToGroup(userId, userIdObject, addUserToGroupPath, client, admin_username, admin_password)
    }
    if ((options?.removeFromGroup && !options?.inputfile) || (!options?.removeFromGroup && options?.inputfile)) {
        removeUserFromGroup(userId, userIdObject, removeUserFromGroupPath, client, admin_username, admin_password)
    }
}

def updateOrganismPermission(String userId, JSONObject userIdObject, String path, RESTClient client, String username, String password) {
    def userArgument = [
            userId: userId,
            organism: userIdObject.organism,
            ADMINISTRATE: userIdObject.ADMINISTRATE,
            EXPORT: userIdObject.EXPORT,
            READ: userIdObject.READ,
            WRITE: userIdObject.WRITE,
            username: username,
            password: password
    ]
    def response = client.post(
            contentType: 'text/javascript',
            path: path,
            body: userArgument
    )
    
    if (response.data.error) {
        println "Error while altering permissions for userId: ${userId}\n${response.data.error}"
    }
    assert response.status == 200
}

def addUserToGroup(String userId, JSONObject userIdObject, String path, RESTClient client, String username, String password) {
    for (String group : userIdObject.addtogroup) {
        if (group == '' || group == null) {continue}
        def userArgument = [
                userId: userId,
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
            println "Error while adding userId ${user.userId} to group ${group}\n${response.data.error}"
        }
        assert response.status == 200
    }
}

def removeUserFromGroup(String userId, JSONObject userIdObject, String path, RESTClient client, String username, String password) {
    for (String group : userIdObject.removefromgroup) {
        if (group == '' || group == null) {continue}
        def userArgument = [
                userId: userId,
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
            println "Error while removing userId ${user.userId} from group ${group}\n${response.data.error}"
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
        String userId = fields[0]
        if (permissionMap.containsKey(userId)) {
            println "Duplicate entries for userId: ${userId}"
        }
        else {
            permissionMap.put(userId, userPermissionObject)
        }
        String organism = fields[1]
        def permissionArray = fields[2].split(':')
        permissionArray.each { 
            if (it == 'ADMINISTRATE') {
                permissionMap.get(userId).ADMINISTRATE = true
            }
            else if (it == 'EXPORT') {
                permissionMap.get(userId).EXPORT = true
            }
            else if (it == 'READ') {
                permissionMap.get(userId).READ = true
            }
            else if (it == 'WRITE') {
                permissionMap.get(userId).WRITE = true
            }
            else {
                println "Unrecognized permission found for userId: ${userId}"
                System.exit(1)
            }
        }
        permissionMap.get(userId).organism = organism
        permissionMap.get(userId).addtogroup = fields[3].split(':')
        permissionMap.get(userId).removefromgroup = fields[4].split(':')
    }
    return permissionMap
}