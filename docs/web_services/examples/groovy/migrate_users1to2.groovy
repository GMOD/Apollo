#!/usr/bin/env groovy
evaluate(new File("./Apollo2Operations.groovy"))

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient
import org.apache.commons.lang.RandomStringUtils


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')


String usageString = "migrate_users1to2.groovy <options>" +
        "Example: \n" +
        "./migrate_users1to2.groovy -inputfile somefile.csv -username ndunn@me.com -password demo  -destinationurl http://localhost:8080/WebApollo2/ "

def cli = new CliBuilder(usage: 'migrate_users.groovy <options>')
cli.setStopAtNonOption(true)
cli.inputfile('A csv file <email>,<firstname>,<lastname>,<password - if empty, random>', required: true, args: 1)
cli.destinationurl('URL of WebApollo 2.0.x instance to which users are to be loaded', required: true, args: 1)
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
    user.password = fields.size() > 2 ? fields[3] : RandomStringUtils.random(10)

    usersArray.add(user)
}

URL url = new URL(options.destinationurl)

def client = new RESTClient(options.destinationurl)

String fullPath = "${url.path}/user/createUser"

for (user in usersArray) {
    println "what is in user ${user}"
    def userArgument = [
            email    : user.email,
            firstName: user.firstName,
            lastName : user.lastName,
            username : options.username,
            password : options.password

    ]
    println "user array ${userArgument}"

    def resp = client.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: [username:options.username,data:userArgument]
    )

    assert resp.status == 200  // HTTP response code; 404 means not found, etc.
    println resp.getData()
}


