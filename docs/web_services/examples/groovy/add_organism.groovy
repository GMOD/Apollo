#!/usr/bin/env groovy

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')

import groovyx.net.http.RESTClient
import org.json.JSONObject


String usageString = "add_organism.groovy <options>\n" +
        "Example: " +
        "./add_organism.groovy -name yeast -url http://localhost:8080/apollo/ -directory /opt/apollo/yeast -public\n"+
        "which would prompt for user/pass\n"+
        "-or-\n"+
        "./add_organism.groovy -name yeast -url http://localhost:8080/apollo/ -directory /opt/apollo/yeast -username user@site.com -password secret -public"

def cli = new CliBuilder(usage: usageString)
cli.setStopAtNonOption(true)
cli.url('URL to Apollo instance', required: true, args: 1)
cli.name('organism common name', required: true, args: 1)
cli.directory('jbrowse data directory', required: true, args: 1)
cli.blatdb('blatdb directory', args: 1)
cli.genus('genus', args: 1)
cli.public('public', args: 0)
cli.species('species', args: 1)
cli.username('username', required: false, args: 1)
cli.password('password', required: false, args: 1)
cli.returnAllOrganisms('returnAllOrganisms (default true)', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options
def admin_username
def admin_password 
try {
    options = cli.parse(args)

    if (!(options?.url && options?.name && options?.directory)) {
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
        commonName: options.name,
        directory : options.directory,
        username  : admin_username,
        password  : admin_password,
        blatdb    : options.blatdb ?: null,
        genus     : options.genus ?: null,
        species   : options.species ?: null,
        publicMode: options.public,
        returnAllOrganisms : options.returnAllOrganisms ?: true 
]

def client = new RESTClient(options.url)
if (options.ignoressl) { client.ignoreSSLIssues() }
String fullPath = "${url.path}/organism/addOrganism"

def resp = client.post(
        contentType: 'text/javascript',
        path: fullPath,
        body: argumentsArray
)

assert resp.status == 200  // HTTP response code; 404 means not found, etc.
println resp.getData()
