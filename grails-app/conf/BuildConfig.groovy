grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"
//def gebVersion = '0.9.3'
//def seleniumVersion = '2.21.0'

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo "http://maven.crbs.ucsd.edu/nexus/content/repositories/NIF-snapshot/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
        runtime 'mysql:mysql-connector-java:5.1.29'
        runtime 'org.postgresql:postgresql:9.3-1101-jdbc41'
        compile 'commons-codec:commons-codec:1.2'
        test "org.grails:grails-datastore-test-support:1.0-grails-2.4"

        compile 'org.json:json:20140107'
        compile 'com.sleepycat:je:5.0.73'
        compile 'org.hibernate:hibernate-tools:3.2.0.ga'

        compile  'edu.sdsc:scigraph-core:1.1-SNAPSHOT'


//        test "org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion"
//        test "org.seleniumhq.selenium:selenium-firefox-driver:$seleniumVersion"
//        test "org.seleniumhq.selenium:selenium-htmlunit-driver:$seleniumVersion"
//        test "org.seleniumhq.selenium:selenium-support:$seleniumVersion"
//
//        test "org.gebish:geb-spock:$gebVersion"
    }

    plugins {
        // plugins for the build system only
        build ":tomcat:7.0.55"

        // plugins for the compile step
        compile ":scaffolding:2.1.2"
        compile ':cache:1.1.7'
        compile ":asset-pipeline:1.9.6"

        // plugins needed at runtime but not for compilation
        runtime ":hibernate4:4.3.5.5" // or ":hibernate:3.6.10.17"
        runtime ":database-migration:1.4.0"
        runtime ":jquery:1.11.1"


        // Uncomment these to enable additional asset-pipeline capabilities
        //compile ":sass-asset-pipeline:1.9.0"
        //compile ":less-asset-pipeline:1.10.0"
        //compile ":coffee-asset-pipeline:1.8.0"
        //compile ":handlebars-asset-pipeline:1.3.0.3"

        compile ":spring-websocket:1.1.0"
        compile ":shiro:1.2.1"

//        compile ":crypto:2.0"

        //compile ":kickstart-with-bootstrap:1.1.0"
//        compile ":twitter-bootstrap:3.1.1.3"
//        compile ":audit-logging:1.0.0"
//        compile ":jasypt-encryption:1.1.0"
//        compile ":joda-time:1.4"

//        test    ":geb:$gebVersion"
    }
}
