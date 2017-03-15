#!/usr/bin/env groovy
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
evaluate(new File("${scriptDir}/Apollo2Operations.groovy"))
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import groovyx.net.http.RESTClient

@Grab(group = 'org.json', module = 'json', version = '20140107')
@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.2')
@Grab(group = 'org.apache.commons', module = 'commons-lang3', version = '3.0')


String usageString = "add_users.groovy <options>\n" +
        "Example: \n" +
        "./add_users.groovy -inputfile somefile.csv -destinationurl http://localhost:8080/Apollo/\n" +
        "./add_users.groovy -firstName New -lastName User -newuser newuser@test.com -newpassword newuserpass -destinationurl http://localhost:8080/Apollo/\n"

def cli = new CliBuilder(usage: 'add_users.groovy <options>')
cli.setStopAtNonOption(true)
cli.inputfile('CSV file with format <email>,<firstname>,<lastname>,<password>,<role>', required: false, args: 1)
cli.username('Admin password', required: false, args: 1)
cli.password('Admin username', required: false, args: 1)
cli.firstName('firstName', required: false, args: 1)
cli.lastName('lastName', required: false, args: 1)
cli.newuser('New user name (if not from csv)',required: false, args: 1)
cli.newpassword('New user password (if not from csv)',required: false, args: 1)
cli.newrole('New user role (if not from csv)',required: false, args: 1)
cli.destinationurl('Apollo URL', required: true, args: 1)
cli.ignoressl('Use this flag to ignore SSL issues', required: false)
OptionAccessor options

def admin_username
def admin_password
try {
    options = cli.parse(args)

    if (!(options?.destinationurl)) {
        println "Requires destination URL\n" + usageString
        return
    }

    if(!options?.inputfile && !options?.newuser) {
        println "Requires CSV or user on command line\n" + usageString
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


JSONArray usersArray = new JSONArray()

if(options.inputfile) {
    new File(options.inputfile).splitEachLine(",") { fields ->
        JSONObject user = new JSONObject()
        user.email = fields[0]
        user.firstName = fields[1]
        user.lastName = fields[2]
        user.password = fields[3] ?: 'default'
        user.role = fields[4] ?: 'user'
        usersArray.add(user)
    }
}
else {
    JSONObject user = new JSONObject()
    user.email = options?.newuser
    user.password = options?.newpassword ?: 'default'
    user.role = options?.newrole ?: 'user'
    user.firstName = options?.firstName ?: 'N/A'
    user.lastName = options?.lastName ?: 'N/A'
    usersArray.add(user)
}


def s=options.destinationurl
if (s.endsWith("/")) {
    s = s.substring(0, s.length() - 1);
}

URL url = new URL(s)

def client = new RESTClient(options.destinationurl)
if (options.ignoressl) { client.ignoreSSLIssues() }
String fullPath = "${url.path}/user/createUser"

for (user in usersArray) {
    def userArgument = [
        email    : user.email,
        firstName: user.firstName,
        lastName : user.lastName,
        role     : user.role,
        username : admin_username,
        password : admin_password,
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


