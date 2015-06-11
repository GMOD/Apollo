#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
evaluate(new File("${scriptDir}/Apollo2Operations.groovy"))

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')


String usageString = "add_users.groovy <options>" +
        "Example: \n" +
        "./add_users.groovy -inputfile somefile.csv -username ndunn@me.com -password demo  -destinationurl http://localhost:8080/WebApollo2/ "

def cli = new CliBuilder(usage: 'add_users.groovy <options>')
cli.setStopAtNonOption(true)
cli.inputfile('A csv file <email>,<firstname>,<lastname>,<password>,<role>', required: true, args: 1)
cli.destinationurl('URL of WebApollo 2.0 instance to which users are to be loaded', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: true, args: 1)
OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.destinationurl && options?.inputfile && options?.username && options?.password)) {
        println "\n" + usageString
        return
    }
} catch (e) {
    println(e)
    return
}

JSONArray usersArray = new JSONArray()
new File(options.inputfile).splitEachLine(",") { fields ->
    JSONObject user = new JSONObject()
    user.email = fields[0]
    user.firstName = fields[1]
    user.lastName = fields[2]
    user.password = fields[3] ?: 'default'
    user.role = fields[4] ?: 'user'

    usersArray.add(user)
}


def s=options.destinationurl
if (s.endsWith("/")) {
        s = s.substring(0, s.length() - 1);
}

URL url = new URL(s)

def client = new RESTClient(options.destinationurl)

String fullPath = "${url.path}/user/createUser"

for (user in usersArray) {
    def userArgument = [
            email    : user.email,
            firstName: user.firstName,
            lastName : user.lastName,
            role     : user.role,
            username : options.username,
            password : options.password,
            newPassword: user.password
    ]

    def resp = client.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: userArgument
    )

    if(resp.data.error) println user.email+": "+resp.data.error
    assert resp.status == 200  // HTTP response code; 404 means not found, etc.
}


