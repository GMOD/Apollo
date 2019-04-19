#!/usr/bin/env groovy

import groovyx.net.http.RESTClient
import net.sf.json.JSONObject

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')

String usageString = "\nUSAGE: add_group.groovy <options>\n" +
        "Example:\n" +
        "./add_group.groovy -name group1 -url http://localhost:8080/Apollo\n" +
        "./add_group.groovy -name group1,group2,group3 -url http://localhost:8080/Apollo\n"

def cli = new CliBuilder(usage: usageString)
cli.setStopAtNonOption(true)
cli.name('group name that may be a comma separated list', required: true, args: 1)
cli.url('Apollo URL', required: true, args: 1)
cli.adminusername('Admin username', required: false, args: 1)
cli.adminpassword('Admin password', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options


def admin_username
def admin_password

try {
    options = cli.parse(args)
    if (options.url == null) {
        println "NOTE: Requires URL\n" + usageString
        return
    }
    if (options.name == null) {
        println "NOTE: Requires a group name, organism and permissions as arguments\n" + usageString
        return
    }

    def sysConsole = System.console()
    if (!(admin_username = options?.adminusername)) {
        admin_username = new String(sysConsole.readLine('Enter admin username: '))
    }
    if (!(admin_password = options?.adminpassword)) {
        admin_password = new String(sysConsole.readPassword('Enter admin password: '))
    }
} catch (e) {
    println(e)
    return
}

JSONObject groupObject = new JSONObject()
groupObject.name = options.name

def s = options.url
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1)
}

URL url = new URL(s)
def client = new RESTClient(options.url)
if (options.ignoressl) {
    client.ignoreSSLIssues()
}
String path = "${url.path}/group/createGroup"

println "Processing group: ${groupObject.name}"
def userArgument = [
        name    : groupObject.name,
        username: admin_username,
        password: admin_password
]
def response = client.post(
        contentType: 'text/javascript',
        path: path,
        body: userArgument
)
if (response.status != 200) {
    if (response.data.error != null) {
        println "Error while ading group: ${userArgument.name}\n${response.data.error}"
    }
}
println "added ${groupObject.name}"

