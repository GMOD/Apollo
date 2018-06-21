// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

extraSrcDirs = "$basedir/src/gwt/org.bbop.apollo.gwt.shared"
eventCompileStart = {
    projectCompiler.srcDirectories << extraSrcDirs
}

grails.config.locations = [
        "file:./${appName}-config.groovy"        // dev only
        , "classpath:${appName}-config.groovy"    // for production deployment
        , "classpath:${appName}-config.properties"
]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }


grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [ // the first one is the default format
                      all          : '*/*', // 'all' maps to '*' or the first available format in withFormat
                      atom         : 'application/atom+xml',
                      css          : 'text/css',
                      csv          : 'text/csv',
                      pdf          : 'application/pdf',
                      rtf          : 'application/rtf',
                      excel        : 'application/vnd.ms-excel',
                      ods          : 'application/vnd.oasis.opendocument.spreadsheet',
                      all          : '*/*',
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
grails.enable.native2ascii = false
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

grails.cache.config = {
    // avoid ehcache naming conflict to run multiple WA instances
    provider {
        name "ehcache-apollo-" + (new Date().format("yyyyMMddHHmmss"))
    }
    cache {
        enabled = true
        name 'globalcache'
        eternal false
        overflowToDisk true
        maxElementsInMemory 10000
        maxElementsOnDisk 10000000
    }
    defaultCache {
        maxElementsInMemory 10000
        eternal false
        timeToIdleSeconds 120
        timeToLiveSeconds 120
        overflowToDisk true
        maxElementsOnDisk 10000000
        diskPersistent false
        diskExpiryThreadIntervalSeconds 120
        memoryStoreEvictionPolicy 'LRU'
    }
}



environments {
    development {
        grails.logging.jul.usebridge = true
        grails.assets.minifyJs = false
        grails.assets.minifyCss = false
        grails.assets.enableSourceMaps = true
        grails.assets.bundle = false
    }
    test {
        grails.assets.minifyJs = false
        grails.assets.minifyCss = false
        grails.assets.enableSourceMaps = true
        grails.assets.bundle = false
    }
    production {
        grails.logging.jul.usebridge = false
        grails.assets.minifyJs = false
        grails.assets.minifyCss = false
        grails.assets.enableSourceMaps = true
        grails.assets.bundle = false
    }
}

// log4j configuration
log4j.main = {
    // log errors from dependencies
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

    // enable logging of our webapollo instance (uncomment debug for extensive output)
    warn 'grails.app'
//    debug 'grails.app'
//    debug 'liquibase'
//    debug 'org.bbop.apollo'

    // more find grained logging
    //trace 'org.hibernate.type'
    //debug 'org.hibernate.SQL'
    //debug 'grails.app'
    //debug 'grails.app.controllers.org.bbop.apollo'
    //debug 'grails.app.controllers.org.bbop.apollo.JbrowseController'
    //debug 'grails.app.services.org.bbop.apollo.FeatureService'
    info 'grails.app.controllers.org.bbop.apollo.GroupController'
    info 'grails.app.controllers.org.bbop.apollo.UserController'
    //info 'grails.app.services'
    //debug 'grails.app.jobs'
    //debug 'grails.app.taglib'
    //debug 'grails.app.controllers'
    //debug 'grails.app.services'
}

//grails.gorm.default.constraints = {
//    '*'(nullable: true)
//}
//grails.datastore.gorm.GormInstanceApi.copy = cloneForDomains ;
grails.gorm.failOnError = true
grails.tomcat.nio = true
grails.tomcat.scan.enabled = true

// default apollo settings
apollo {
    default_minimum_intron_size = 1
    history_size = 0
    overlapper_class = "org.bbop.apollo.sequence.OrfOverlapper"
    track_name_comparator = "/config/track_name_comparator.js"
    use_cds_for_new_transcripts = false
    transcript_overlapper = "CDS"
    feature_has_dbxrefs = true
    feature_has_attributes = true
    feature_has_pubmed_ids = true
    feature_has_go_ids = true
    feature_has_comments = true
    feature_has_status = true
    export_subfeatures_attr = false
    user_pure_memory_store = true
    is_partial_translation_allowed = false // unused so far
    export_subfeature_attrs = false

    // used for uploading
    common_data_directory = "/opt/apollo"

    // settings for Chado export
    // set chado_export_fasta_for_sequence if you want the reference sequence FASTA to be exported into the database
    // Note: Enabling this feature can be memory intensive
    chado_export_fasta_for_sequence = false
    // set chado_export_fasta_for_cds if you want the CDS FASTA to be exported into the database
    chado_export_fasta_for_cds = false
    only_owners_delete = false

    // this is the default
    // other translation codes are of the form ncbi_KEY_translation_table.txt
    // under the web-app/translation_tables  directory
    // to add your own add them to that directory and over-ride the translation code here
    get_translation_code = 1
    proxies = [
            [
                    referenceUrl : 'http://golr.geneontology.org/select',
                    targetUrl    : 'http://golr.geneontology.org/solr/select',
                    active       : true,
                    fallbackOrder: 0,
                    replace      : true
            ]
            ,
            [
                    referenceUrl : 'http://golr.geneontology.org/select',
                    targetUrl    : 'http://golr.berkeleybop.org/solr/select',
                    active       : false,
                    fallbackOrder: 1,
                    replace      : false
            ]
    ]
    sequence_search_tools = [
            blat_nuc : [
                    search_exe  : "/usr/local/bin/blat",
                    search_class: "org.bbop.apollo.sequence.search.blat.BlatCommandLineNucleotideToNucleotide",
                    name        : "Blat nucleotide",
                    params      : ""
            ],
            blat_prot: [
                    search_exe  : "/usr/local/bin/blat",
                    search_class: "org.bbop.apollo.sequence.search.blat.BlatCommandLineProteinToNucleotide",
                    name        : "Blat protein",
                    params      : ""
                    //tmp_dir: "/opt/apollo/tmp" optional param
            ]
    ]
    data_adapters = [[
                             permission   : 1,
                             key          : "GFF3",
                             data_adapters: [[
                                                     permission: 1,
                                                     key       : "Only GFF3",
                                                     options   : "output=file&format=gzip&type=GFF3&exportGff3Fasta=false"
                                             ],
                                             [
                                                     permission: 1,
                                                     key       : "GFF3 with FASTA",
                                                     options   : "output=file&format=gzip&type=GFF3&exportGff3Fasta=true"
                                             ]]
                     ],
                     [
                             permission   : 1,
                             key          : "VCF",
                             data_adapters: [[
                                                     permission: 1,
                                                     key       : "VCF",
                                                     options   : "output=file&format=gzip&type=VCF"
                                             ]
                             ]
                     ],
                     [
                             permission   : 1,
                             key          : "FASTA",
                             data_adapters: [[
                                                     permission: 1,
                                                     key       : "peptide",
                                                     options   : "output=file&format=gzip&type=FASTA&seqType=peptide"
                                             ],
                                             [
                                                     permission: 1,
                                                     key       : "cDNA",
                                                     options   : "output=file&format=gzip&type=FASTA&seqType=cdna"
                                             ]
                                             ,
                                             [
                                                     permission: 1,
                                                     key       : "CDS",
                                                     options   : "output=file&format=gzip&type=FASTA&seqType=cds"
                                             ]
                                             ,
                                             [
                                                     permission: 1,
                                                     key       : "highlighted region",
                                                     options   : "output=file&format=gzip&type=FASTA&seqType=genomic"
                                             ]
                             ]

                     ]]

    // TODO: should come from config or via preferences database
    splice_donor_sites = ["GT"]
    splice_acceptor_sites = ["AG"]
    gff3.source = "."
    bootstrap = false

    info_editor = {
        feature_types = "default"
        attributes = true
        dbxrefs = true
        pubmed_ids = true
        go_ids = true
        comments = true
    }

    // customize admin tab on annotator panel with these links
    administrativePanel = [
            ['label': "Canned Comments", 'link': "/cannedComment/",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Canned Key", 'link': "/cannedKey/",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Canned Values", 'link': "/cannedValue/",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Feature Types", 'link': "/featureType/",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Statuses", 'link': "/availableStatus/",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Proxies", 'link': "/proxy/",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Report::Organisms", 'link': "/organism/report/", 'type': "report"]
            , ['label': "Report::Sequences", 'link': "/sequence/report/", 'type': "report"]
            , ['label': "Report::Annotator", 'link': "/annotator/report/", 'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Report::Instructor", 'link': "/annotator/instructorReport/", 'type': "report"]
            , ['label': "Report::Changes", 'link': "/featureEvent/report/", 'type': "report"]
            , ['label': "System Info", 'link': "/home/systemInfo/", 'type': "report",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "Performance Metrics", 'link': "/home/metrics/", 'type': "report",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
            , ['label': "WebServices", 'link': "/WebServices/", 'type': "report",'globalRank':org.bbop.apollo.gwt.shared.GlobalPermissionEnum.ADMIN]
    ]

    // over-ride in apollo-config.groovy to add extra tabs
    extraTabs = [
//            ['title': 'extra1', 'url': 'http://localhost:8080/apollo/annotator/report/'],
//            ['title': 'extra2', 'content': '<b>Some content</b><a href="http://google.com" target="_blank">Google</a>']
    ]


    authentications = [
            ["name"     : "Username Password Authenticator",
             "className": "usernamePasswordAuthenticatorService",
             "active"   : true,
            ]
            ,
            ["name"     : "Remote User Authenticator",
             "className": "remoteUserAuthenticatorService",
             "active"   : false,
            ]
    ]

    // comment out if you don't want this to be reported
    google_analytics = ["UA-62921593-1"]

    phone {
        phoneHome = true
        url = "https://s3.amazonaws.com/"
        bucketPrefix = "apollo-usage-"
        fileName = "ping.json"
    }

    native_track_selector_default_on = false
}


grails.plugin.databasemigration.updateOnStart = true
grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']

// from: http://grails.org/plugin/audit-logging
auditLog {
    //note, this disables the audit log
    disabled = true
    //verbose = true // verbosely log all changed values to db
    logIds = true  // log db-ids of associated objects.

}

// Default JBrowse configuration
jbrowse {
    git {
        url = "https://github.com/gmod/jbrowse"
//        branch = "master"
        tag = "maint/1.12.5-apollo"
//        tag = "27ec453"
        alwaysPull = false
        alwaysRecheck = false
    }
//    url {
//        // always use dev for apollo
//        url = "http://jbrowse.org/wordpress/wp-content/plugins/download-monitor/download.php?id=102"
//        type ="zip"
//        fileName = "JBrowse-1.12.0-dev"
//    }
//
//	// Warning: We are still testing the performance of the NeatFeatures plugins in combination with Apollo.
//	// We advise caution if enabling these plugins with Apollo until this process is finalized.
    plugins {
        WebApollo {
            included = true
        }
//        NeatHTMLFeatures{
//            included = true
//            linearGradient = 0
//        }
//        NeatCanvasFeatures{
//            included = true
//        }
        RegexSequenceSearch {
            included = true
        }
        HideTrackLabels {
            included = true
        }
//        MyVariantInfo {
//            git = 'https://github.com/GMOD/myvariantviewer'
//            branch = 'master'
//            alwaysRecheck = "true"
//            alwaysPull = "true"
//        }
//        SashimiPlot {
//            git = 'https://github.com/cmdcolin/sashimiplot'
//            branch = 'master'
//            alwaysPull = "true"
//        }
    }

}
