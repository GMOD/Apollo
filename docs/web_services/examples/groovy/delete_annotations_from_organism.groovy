#!/usr/bin/env groovy
import groovyjarjarcommonscli.Option
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient

@Grab(group = 'org.json', module = 'json', version = '20140107')
//@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.1')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.5.2')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')

String usageString = "\nUSAGE: delete_annotations_from_organism.groovy <options>\n" +
        "Example (will prompt if 'adminusername' and 'adminpassword' are not provided):\n" +
        "./delete_annotations_from_organism.groovy -organismname organism_name -destinationurl http://localhost:8080/Apollo\n" +
        "./delete_annotations_from_organism.groovy -organismid 123  -sequences chr1,chr2 -destinationurl http://localhost:8080/Apollo -adminusername bob@gov.com -adminpassword demo\n"  +
        "./delete_annotations_from_organism.groovy -organismid 123 -destinationurl http://localhost:8080/Apollo\n" +
        "./delete_annotations_from_organism.groovy -organismid 123 -destinationurl http://localhost:8080/Apollo -adminusername bob@gov.com -adminpassword demo"

def cli = new CliBuilder(usage: 'delete_annotations_from_organism.groovy')
cli.setStopAtNonOption(true)
cli.organismid('Organism Id corresponding to organism', required: false, args: 1)
cli.organismname('Common name for the organism (if organismid is not provided)', required: false, args:1)
cli.destinationurl('Apollo URL', required: true, args: 1)
cli.sequences('Sequence names from the organism, comma-delimited (e.g., chr1,chr2,chr3)', required: false, args:1)
cli.adminusername('Admin username', required: false, args: 1)
cli.adminpassword('Admin password', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options

def admin_username
def admin_password

try {
    options = cli.parse(args)
    if (!options?.destinationurl) {
        println "NOTE: Requires destination URL\n" + usageString
        return
    }
    if (!options?.organismid && !options?.organismname) {
        println "NOTE: Requires organismid or organismname as an argument\n" + usageString
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

def s=options.destinationurl
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1)
}

URL url = new URL(s)
def client = new RESTClient(options.destinationurl)
if (options.ignoressl) { client.ignoreSSLIssues() }
String path = "${url.path}/organism/deleteOrganismFeatures"


def userArgument = [
        organism: options.organismid ? options.organismid : options.organismname,
        username: admin_username,
        password: admin_password
]
if(options.sequences){
    userArgument.sequences = options.sequences
}

def response = client.post(
        contentType: 'text/javascript',
        path: path,
        body: userArgument
)

if (response.data.error) {
    println "Error while deleting features for organism\n${response.data.error}"
}
assert response.status == 200
