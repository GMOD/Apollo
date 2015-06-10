#!/usr/bin/env groovy
evaluate(new File("./Apollo1Operations.groovy"))
evaluate(new File("./Apollo2Operations.groovy"))

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "migrate_users1to2.groovy <options>" +
        "Example: \n" +
        "./migrate_users1to2.groovy -username1 web_apollo_users_admin -password1 somepass -databaseurl localhost/web_apollo_users -username2 ndunn@me.com -password2 demo  -organism Honey2 -destinationurl http://localhost:8080/WebApollo2/ "

def cli = new CliBuilder(usage: 'migrate_users.groovy <options>')
cli.setStopAtNonOption(true)
cli.databaseurl('URL of WebApollo 1.0.x database from which users are fetched', required: true, args: 1)
cli.destinationurl('URL of WebApollo 2.0.x instance to which users are to be loaded', required: true, args: 1)
cli.username1('username1', required: true, args: 1)
cli.password1('password1', required: false , args: 1)
cli.username2('username2', required: true, args: 1)
cli.password2('password2', required: true, args: 1)
OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.destinationurl && options?.databaseurl && options?.username2 && options?.password2 && options?.username1)) {
        println "\n"+usageString
        return
    }
} catch (e) {
    println(e)
    return
}

def users = Apollo1Operations.getUsers(options.username1, options?.password1 ?: "",options.databaseurl)
if (users == null) {
    println "Could not communicate with ${options.databaseurl}"
    return
}

JSONObject newArray = new JSONObject()
JSONArray addUsersArray = new JSONArray()

URL url = new URL(options.destinationurl)

def argumentsArray = [
        username  : options.username2,
        password  : options.password2
]

println "arguments array = ${argumentsArray}"

def client = new RESTClient(options.destinationurl)

String fullPath = "${url.path}/user/createUser"

for(user in users){
    def userArray = [
//          email: user
    ]
    userArray << argumentsArray

    def resp = client.post(
            contentType: 'text/javascript',
            path: fullPath,
            body: userArray
    )

    assert resp.status == 200  // HTTP response code; 404 means not found, etc.
    println resp.getData()
}


