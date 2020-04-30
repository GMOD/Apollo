// default username/password on h2 are given here. see docs for adjusting
dataSource {
    pooled = true
    jmxExport = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
// environment specific settings
environments {
    development {
        // sample config to turn on debug logging in development e.g. for apollo run-local
        log4j.main = {
            debug "grails.app"
        }
        // sample config to edit apollo specific configs in development mode
        apollo {
            gff3.source = "testing"
        }
        dataSource {
            // NOTE: this is in memory, so it will be deleted.
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            url = "jdbc:h2:AnnotationDatabase:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    test {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }

    //note: not necessarily recommended to use h2 in production mode. see precautions
    production {
        dataSource {
            dbCreate = "update"
            //NOTE: production mode uses file instead of mem database
            //NOTE: Please specify the appropriate file path, otherwise /tmp/prodDb will be used.
            url = "jdbc:h2:/opt/apollo/h2/prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            properties {
               // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
               jmxEnabled = true
               initialSize = 5
               maxActive = 50
               minIdle = 5
               maxIdle = 25
               maxWait = 10000
               maxAge = 10 * 60000
               timeBetweenEvictionRunsMillis = 5000
               minEvictableIdleTimeMillis = 60000
               validationQuery = "SELECT 1"
               validationQueryTimeout = 3
               validationInterval = 15000
               testOnBorrow = true
               testWhileIdle = true
               testOnReturn = false
               jdbcInterceptors = "ConnectionState"
               defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
            }
        }
    }
}

//apollo {
//    gff3.source = "MyOrg" // also for GPAD export
//    only_owners_delete = true
//    common_data_directory = "/opt/temporary/apollo"
//    store_orig_id = false
//    count_annotations = false
//    fa_to_twobit_exe = "/usr/local/bin/blat" // get form https://genome.ucsc.edu/goldenPath/help/blatSpec.html
//    sequence_search_tools {
//        blat_nuc {
//            search_exe = "/usr/local/bin/blastn"
//            search_class = "org.bbop.apollo.sequence.search.blast.BlastCommandLine"
//            name = "Blast nucleotide"
//            params = ""
//        }
//        blat_prot {
//            search_exe = "/usr/local/bin/tblastn"
//            search_class = "org.bbop.apollo.sequence.search.blast.BlastCommandLine"
//            name = "Blast protein to translated nucleotide"
//            params = ""
//            //tmp_dir: "/opt/apollo/tmp" optional param
//        }
//    }
//    extraTabs = [
//            ['title': 'extra1', 'url': 'http://localhost:8080/apollo/annotator/report/'],
//            ['title': 'extra2', 'content': '<b>Apollo</b> documentation <a href="http://genomearchitect.org" target="_blank">linked here</a>']
//            ['title': 'GGA', 'url': 'https://gitter.im/galaxy-genome-annotation/Lobby/~embed']
//            ['title': 'JBrowse', 'url': 'https://gitter.im/GMOD/jbrowse/~embed']
//    ]
//}

// Uncomment to change the default memory configurations
//grails.project.fork = [
//        test   : false,
//        // configure settings for the run-app JVM
//        run    : [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 1024, forkReserve: false],
//        // configure settings for the run-war JVM
//        war    : [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 1024, forkReserve: false],
//        // configure settings for the Console UI JVM
//        console: [maxMemory: 2048, minMemory: 64, debug: false, maxPerm: 1024]
//]

// Uncomment to make changes
//
//jbrowse {
//    git {
//        url= "https://github.com/GMOD/jbrowse"
//        tag = "1.16.5-release"
////        branch = "dev"
////        hash = "09b71099bf73c50d37a0e911baf06b4975e3f6ca"
//        alwaysPull = true
//        alwaysRecheck = true
//    }
//    plugins {
//        NeatHTMLFeatures{
//            included = true
//        }
//        NeatCanvasFeatures{
//            included = true
//        }
//        RegexSequenceSearch{
//            included = true
//        }
//        HideTrackLabels{
//            included = true
//        }
////        MyVariantInfo {
////            git = 'https://github.com/GMOD/myvariantviewer'
////            branch = 'master'
////            alwaysRecheck = "true"
////            alwaysPull = "true"
////        }
////        SashimiPlot {
////            git = 'https://github.com/cmdcolin/sashimiplot'
////            branch = 'master'
////            alwaysPull = "true"
////        }
//    }
//}
