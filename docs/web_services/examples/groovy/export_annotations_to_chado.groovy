#!/usr/bin/env groovy

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')

import groovyx.net.http.RESTClient
import org.json.JSONObject

String usageString = "Example: " +
        "./export_annotations_to_chado.groovy -organism Amel -url http://localhost:8080/apollo\n" +
        "which would prompt username and password\n"
        "-or-\n" +
        "./export_annotations_to_chado.groovy -username user@site.com -password secret -organism Amel -url http://localhost:8080/apollo\n"

def cli = new CliBuilder(usage: "export_annotations_to_chado.groovy <options>")
cli.setStopAtNonOption(true)
cli.organism('Organism common name', required: true, args: 1)
cli.url('URL to Apollo instance', required: true, args: 1)
cli.username('username', required: false, args: 1)
cli.password('password', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)

OptionAccessor options
def admin_username
def admin_password

try {
        options = cli.parse(args)
        if (!(options.url && options.organism)) {
                println usageString
                return
        }

        def cons = System.console()
        if (!(admin_username = options?.username)) {
                admin_username = new String(cons.readPassword('Enter admin username: '))
        }
        if (!(admin_password = options?.password)) {
                admin_password = new String(cons.readPassword('Enter admin password: '))
        }
} catch (e) {
        println(e)
        return
}

def urlString = options.url
if (urlString.endsWith('/')) {
        urlString = urlString.substring(0, urlString.length() - 1)
}

URL url = new URL(urlString)

def sequencesList = []

def argumentsArray = [
        username: admin_username,
        password: admin_password,
        organism: options.organism,
        type: 'CHADO',
        seqType: '',
        exportGff3Fasta: '',
        output: '',
        format: '',
        sequences: [],
        exportAllSequences: true
]

def client = new RESTClient(options.url)
if (options.ignoressl) { client.ignoreSSLIssues() }
String fullPath = "${url.path}/IOService/write"

def response = client.post(
        contentType: 'text/javascript',
        path: fullPath,
        body: argumentsArray
)

assert response.status == 200
println response.getData()