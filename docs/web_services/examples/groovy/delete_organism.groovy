#!/usr/bin/env groovy

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')

import groovyx.net.http.RESTClient
import org.json.JSONObject


String usageString = "delete_organism.groovy <options>\n" +
        "Example: " +
        "./delete_organism.groovy -name yeast -url http://localhost:8080/apollo/\n"+
        "which would prompt for user/pass\n"+
        "-or-\n"+
        "./delete_organism.groovy -name yeast -url http://localhost:8080/apollo/ -username user@site.com -password secret"

def cli = new CliBuilder(usage: usageString)
cli.setStopAtNonOption(true)
cli.url('URL to Apollo instance', required: true, args: 1)
cli.name('organism common name', required: true, args: 1)
cli.username('username', required: false, args: 1)
cli.password('password', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options
def admin_username
def admin_password 
try {
    options = cli.parse(args)

    if (!(options?.url && options?.name)) {
        return
    }

    def cons = System.console()
    if (!(admin_username=options?.username)) {
        admin_username = new String(cons.readPassword('Enter admin username: ') )
    }
    if (!(admin_password=options?.password)) {
        admin_password = new String(cons.readPassword('Enter admin password: ') )
    }

} catch (e) {
    println(e)
    return
}


def s=options.url
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1);
}

URL url = new URL(s)


def argumentsArray = [
        organism: options.name,
        username  : admin_username,
        password  : admin_password,
]

def client = new RESTClient(options.url)
if (options.ignoressl) { client.ignoreSSLIssues() }


println "Deleting organism"
String fullPath = "${url.path}/organism/deleteOrganism"
def resp = client.post(
        contentType: 'text/javascript',
        path: fullPath,
        body: argumentsArray
)
assert resp.status == 200  // HTTP response code; 404 means not found, etc.
