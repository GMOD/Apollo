#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')

String usageString = "get_new_features.groovy <options>" +
        "Example: \n" +
        "./get_new_features.groovy -username admin -password somepass -url http://localhost/apollo -date 2014-05-17:00:00:00 -afterDate=true -output thisFile.txt"

def cli = new CliBuilder(usage: 'get_fasta.groovy <options>')
cli.setStopAtNonOption(true)
cli.username('username', required: false, args: 1)
cli.password('password', required: false, args: 1)
cli.url('url', required: true, args: 1)
cli.date('date', required: true, args: 1)
cli.afterDate('afterDate', required: false, args: 1)
cli.output('output', required: false, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options
def admin_username
def admin_password
try {
    options = cli.parse(args)

    if (!(options?.url)) {
        println "Requires destination URL\n" + usageString
        return
    }

    def cons = System.console()
    if(cons) {
        if (!(admin_username=options?.username)) {
            admin_username = new String(cons.readPassword('Username: ') )
        }
        if (!(admin_password=options?.password)) {
            admin_password = new String(cons.readPassword('Password: ') )
        }
    }
    else if(!options.username||!options.password){
        System.err.println("Error: missing -username and -password and can't read them when using redirect");
        if(!options.output) throw "Require output file"  
    }
    else {
        admin_username=options.username
        admin_password=options.password
    }
} catch (e) {
    println(e)
    return
}

// just get data
def client = new RESTClient(options.url,'text/plain')
if (options.ignoressl) { client.ignoreSSLIssues() }
def post = [
    username: admin_username,
    password: admin_password,
    date: options.date,
    afterDate: options.afterDate ?: true
]
def response = client.post(path:options.url+'/featureEvent/findChanges',body: post)

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

if(options.output) {
    def file=new File(options.output)
    def writer = new PrintWriter(file)
    writer.println builder.toString();  
    writer.close()
}
else {
    print builder.toString();
}



