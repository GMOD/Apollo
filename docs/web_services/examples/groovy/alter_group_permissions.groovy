#!/usr/bin/env groovy
import groovyjarjarcommonscli.Option
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')

String usageString = "\nUSAGE: alter_group_permissions.groovy <options>\n" +
        "Permissions: ADMINISTRATE,WRITE,EXPORT,READ (lower permissions are inherited)\n" +
        "Example:\n" +
        "./alter_group_permissions.groovy -inputfile group_to_permissions.csv -destinationurl http://localhost:8080/Apollo\n" +
        "./alter_group_permissions.groovy -groupname group1 -organism organism_name -permission READ:WRITE -destinationurl http://localhost:8080/Apollo"

def cli = new CliBuilder(usage: 'alter_group_permissions.groovy')
cli.setStopAtNonOption(true)
cli.inputfile('CSV file with format <groupname>,<organism>,<permissions>', required: false, args: 1)
cli.organism('Organism for which the current group should be granted permissions', required: false, args: 1)
cli.permission('Permission(s) to be granted for group on organism, separated by \':\'', required: false, args: 1)
cli.groupname('groupName for a group', required: false, args: 1)
cli.destinationurl('Apollo URL', required: true, args: 1)
cli.adminusername('Admin username', required: false, args: 1)
cli.adminpassword('Admin password', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options

def admin_username
def admin_password
def groupPermissionMap = [:]

try {
    options = cli.parse(args)
    if (!(options?.destinationurl)) {
        println "NOTE: Requires destination URL\n" + usageString
        return
    }
    if (!options?.inputfile && !options?.groupname) {
        println "NOTE: Requires a CSV as inputfile   OR   groupname, organism and permissions as arguments\n" + usageString
        return
    }
    if (options?.inputfile && options?.groupname) {
        println "NOTE: Requires a CSV as inputfile   OR   groupname, organism and permissions as arguments\n" + usageString
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
    groupPermissionMap = parseInputFile(options.inputfile)
}
else {
    JSONObject groupObject = new JSONObject()
    groupObject.groupname = options.groupname
    groupObject.organism = options.organism
    groupObject.ADMINISTRATE = false
    groupObject.EXPORT = false
    groupObject.READ = false
    groupObject.WRITE = false
    def permissionArray = options?.permission ? options.permission.split(':') : []
    permissionArray.each {
        if (it == 'ADMINISTRATE') {groupObject.ADMINISTRATE = true}
        else if (it == 'EXPORT') {groupObject.EXPORT = true}
        else if (it == 'READ') {groupObject.READ = true}
        else if (it == 'WRITE') {groupObject.WRITE = true}
    }
    groupPermissionMap.put(options.groupname, groupObject)
}

def s=options.destinationurl
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1)
}

URL url = new URL(s)
def client = new RESTClient(options.destinationurl)
if (options.ignoressl) { client.ignoreSSLIssues() }
String path = "${url.path}/group/updateOrganismPermission"

for (String groupName in groupPermissionMap.keySet()) {
    println "Processing group: ${groupName}"
    JSONObject groupObject = groupPermissionMap.get(groupName) as JSONObject
    if ((options?.permission && !options?.inputfile) || (!options?.permission && options?.inputfile)) {
        def userArgument = [
                name: groupObject.groupname,
                organism: groupObject.organism,
                ADMINISTRATE: groupObject.ADMINISTRATE,
                EXPORT: groupObject.EXPORT,
                READ: groupObject.READ,
                WRITE: groupObject.WRITE,
                username: admin_username,
                password: admin_password
        ]
        def response = client.post(
                contentType: 'text/javascript',
                path: path,
                body: userArgument
        )
        if (response.data.error) {
            println "Error while altering permissions for group: ${groupName}\n${response.data.error}"
        }
        assert response.status == 200
    }
}

def parseInputFile(String inputFile) {
    def permissionMap = [:]
    new File(inputFile).splitEachLine(',') { fields ->
        if (fields.size() != 3) {
            println "ERROR: Improper formatting in ${inputFile} at line:\n${fields.join(',')}"
            return
        }
        JSONObject groupPermissionObject = new JSONObject()
        groupPermissionObject.ADMINISTRATE = false
        groupPermissionObject.EXPORT = false
        groupPermissionObject.READ = false
        groupPermissionObject.WRITE = false
        String groupName  = fields[0]
        if (permissionMap.containsKey(groupName)) {
            println "Duplicate entries for groupName: ${groupName}"
        }
        else {
            permissionMap.put(groupName, groupPermissionObject)
        }
        permissionMap.get(groupName).groupname = groupName
        String organism = fields[1]
        def permissionArray = fields[2].split(':')
        permissionArray.each {
            if (it == 'ADMINISTRATE') {
                permissionMap.get(groupName).ADMINISTRATE = true
            }
            else if (it == 'EXPORT') {
                permissionMap.get(groupName).EXPORT = true
            }
            else if (it == 'READ') {
                permissionMap.get(groupName).READ = true
            }
            else if (it == 'WRITE') {
                permissionMap.get(groupName).WRITE = true
            }
            else {
                println "Unrecognized permission found for groupName: ${groupName}"
                System.exit(1)
            }
        }
        permissionMap.get(groupName).organism = organism
    }
    return permissionMap
}