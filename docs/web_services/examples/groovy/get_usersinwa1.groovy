#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
evaluate(new File("${scriptDir}/Apollo1Operations.groovy"))

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient


@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7')

String usageString = "get_usersinwa1.groovy <options>" +
        "Example: \n" +
        "./get_usersinwa1.groovy -username web_apollo_users_admin -password somepass -databaseurl localhost/web_apollo_users "

def cli = new CliBuilder(usage: 'get_usersinwa1.groovy <options>')
cli.setStopAtNonOption(true)
cli.databaseurl('URL of Apollo 1.0.x database from which users are fetched', required: true, args: 1)
cli.username('username', required: true, args: 1)
cli.password('password', required: false, args: 1)
OptionAccessor options

try {
    options = cli.parse(args)

    if (!(options?.databaseurl && options?.username)) {
        println "\n" + usageString
        return
    }
} catch (e) {
    println(e)
    return
}

// just get data
def users = Apollo1Operations.getUsers(options.username, options?.password ?: "", options.databaseurl)
if (users == null) {
    println "Could not communicate with ${options.databaseurl}"
    return
}
for (user in users) {
    println user.username
}
