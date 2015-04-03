// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

extraSrcDirs = "$basedir/src/gwt/org.bbop.apollo.gwt.shared"
eventCompileStart = {
    projectCompiler.srcDirectories << extraSrcDirs
}

grails.config.locations = [
        "file:./${appName}-config.groovy"
        ,"file:/tmp/${appName}-config.groovy"
        ,"classpath:${appName}-config.groovy"
        ,"classpath:${appName}-config.properties"
]

//grails.assetsminifyJs = true
grails.assets.minifyJs = false
grails.assets.minifyCss = false
grails.assets.enableSourceMaps = true

// this works
grails.assets.bundle=false

//grails.assets.minifyOptions = [
//        languageMode     : 'ES5',
//        targetLanguage   : 'ES5', //Can go from ES6 to ES5 for those bleeding edgers
//        optimizationLevel: 'SIMPLE' //Or ADVANCED or WHITESPACE_ONLY
//]


grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [ // the first one is the default format
                      all          : '*/*', // 'all' maps to '*' or the first available format in withFormat
                      atom         : 'application/atom+xml',
                      css          : 'text/css',
                      csv          : 'text/csv',
                      form         : 'application/x-www-form-urlencoded',
                      html         : ['text/html', 'application/xhtml+xml'],
                      js           : 'text/javascript',
                      json         : ['application/json', 'text/json'],
                      multipartForm: 'multipart/form-data',
                      rss          : 'application/rss+xml',
                      text         : 'text/plain',
                      hal          : ['application/hal+json', 'application/hal+xml'],
                      xml          : ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}


grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart = false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false


environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
}

// log4j configuration
log4j.main = {
    // Example of changing the log pattern for the default console appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error 'org.codehaus.groovy.grails.web.servlet',        // controllers
            'org.codehaus.groovy.grails.web.pages',          // GSP
            'org.codehaus.groovy.grails.web.sitemesh',       // layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping',        // URL mapping
            'org.codehaus.groovy.grails.commons',            // core / classloading
            'org.codehaus.groovy.grails.plugins',            // plugins
            'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

//    trace 'org.hibernate.type'
//    debug 'org.hibernate.SQL'

    info 'grails.app'

//    debug 'grails.app.controllers.org.bbop.apollo'
//    info 'grails.app.controllers.org.bbop.apollo.JbrowseController'
//    info 'grails.app.services'
//    debug 'grails.app.controllers.edu.uoregon.nic.nemo.portal'

//    debug 'grails.app.jobs'
//    debug 'grails.app.taglib'
//    debug 'grails.app.taglib.edu.uoregon.nic.nemo.portal'
//    debug 'grails.app.controllers'
//    debug 'grails.app.services'
//    debug 'grails.app.services.edu.uoregon.nic.nemo.portal.OntologyService'
//    debug 'grails.app.services.edu.uoregon.nic.nemo.portal.DataStubService'
//    debug 'grails.app.services.edu.uoregon.nic.nemo.portal.UserService'
//    debug 'grails.app.controllers.edu.uoregon.nic.nemo.portal'
//    debug 'grails.app.controllers.edu.uoregon.nic.nemo.portal.TermController'
}

//grails.gorm.default.constraints = {
//    '*'(nullable: true)
//}
grails.gorm.failOnError = true
//grails.datastore.gorm.GormInstanceApi.copy = cloneForDomains ;

apollo.jbrowse.data.directory = "/opt/apollo/jbrowse/data"

apollo.default_minimum_intron_size = 1
apollo.history_size = 0
apollo.overlapper_class = "org.bbop.apollo.sequence.OrfOverlapper"
apollo.track_name_comparator = "/config/track_name_comparator.js"
apollo.use_cds_for_new_transcripts = true
apollo.user_pure_memory_store = true
apollo.translation_table = "/config/translation_tables/ncbi_1_translation_table.txt"
apollo.is_partial_translation_allowed = false // unused so far
//apollo.get_translation_code = -1
apollo.get_translation_code = 1

// TODO: should come from config or via preferences database
apollo.splice_donor_sites = [ "GT"]
apollo.splice_acceptor_sites = [ "AG"]
apollo.gff3.source= "."

apollo.info_editor = {
    feature_types = "default"
    attributes = true
    dbxrefs = true
    pubmed_ids = true
    go_ids = true
    comments = true
}

// https://github.com/zyro23/grails-spring-websocket
// websocket info
grails.tomcat.nio = true
grails.tomcat.scan.enabled = true


// from: http://grails.org/plugin/audit-logging
auditLog {
//    // note, this disables the audit log
    disabled = true
//    verbose = true // verbosely log all changed values to db
//    logIds = true  // log db-ids of associated objects.
//    // Note: if you change next 2 properties, you must update your database schema!
////    tablename = 'my_audit' // table name for audit logs.
//    largeValueColumnTypes = true // use large column db types for oldValue/newValue.
////    TRUNCATE_LENGTH = 1000
//    cacheDisabled = true
//    logFullClassName = true
////    replacementPatterns = ["local.example.xyz.":""] // replace with empty string.
////    actorClosure = { request, session ->
////        // SpringSecurity Core 1.1.2
////        if (request.applicationContext.springSecurityService.principal instanceof java.lang.String){
////            return request.applicationContext.springSecurityService.principal
////        }
////        def username = request.applicationContext.springSecurityService.principal?.username
////        if (SpringSecurityUtils.isSwitched()){
////            username = SpringSecurityUtils.switchedUserOriginalUsername+" AS "+username
////        }
////        return username
////    }
}
