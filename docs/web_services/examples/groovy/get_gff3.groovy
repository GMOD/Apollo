#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "get_gff3.groovy <options>" +
        "Example: \n" +
        "./get_gff3.groovy -username admin -password somepass -url http://localhost/apollo "

def cli = new CliBuilder(usage: 'get_gff3.groovy <options>')
cli.setStopAtNonOption(true)
cli.url('URL of WebApollo 2.0 from which gff3 is to be fetched', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: false, args: 1)
cli.password('url', required: false, args: 1)
cli.organism('organism', required: false, args: 1)
OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.url && options?.username && options?.organism)) {
        println "\n" + usageString
        return
    }
} catch (e) {
    println(e)
    return
}

// just get data
println "fetching url: "+options.url
def client = new RESTClient(options.url,'text/plain')
def response = client.post(path:options.url+'/IOService/write',body: [format: 'plain',type: 'GFF3',exportSequence: false,exportAllSequences: true,organism: options.organism, output:'text'])

assert response.status == 200
StringBuilder builder = new StringBuilder();
int charsRead = -1;
char[] chars = new char[100];
charsRead = response.data.read(chars,0,chars.length);
while(charsRead>0){
    //if we have valid chars, append them to end of string.
    builder.append(chars,0,charsRead);
    charsRead = response.data.read(chars,0,chars.length);
}
print builder.toString();

