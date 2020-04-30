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
        dataSource{
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "<CHANGEME>"
            password = "<CHANGEME>"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgresPlusDialect
            url = "jdbc:postgresql://localhost/apollo"
        }
//        dataSource_chado{
//            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
//            username = "<CHANGEME>"
//            password = "<CHANGEME>"
//            driverClassName = "org.postgresql.Driver"
//            dialect = org.hibernate.dialect.PostgresPlusDialect
//            url = "jdbc:postgresql://localhost/apollo-chado"
//        }
    }
    test {
        dataSource{
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "<CHANGEME>"
            password = "<CHANGEME>"
            driverClassName = "org.postgresql.Driver"
//        dialect = org.hibernate.dialect.PostgresPlusDialect
            dialect = "org.bbop.apollo.ImprovedPostgresDialect"
            url = "jdbc:postgresql://localhost/apollo-test"
        }
//        dataSource_chado{
//            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
//            username = "<CHANGEME>"
//            password = "<CHANGEME>"
//            driverClassName = "org.postgresql.Driver"
////        dialect = org.hibernate.dialect.PostgresPlusDialect
//            dialect = "org.bbop.apollo.ImprovedPostgresDialect"
//            url = "jdbc:postgresql://localhost/apollo-test-chado"
//        }
    }
    production {
        dataSource{
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "<CHANGEME>"
            password = "<CHANGEME>"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgresPlusDialect
            url = "jdbc:postgresql://localhost/apollo-production"
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
//        dataSource_chado{
//            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
//            username = "<CHANGEME>"
//            password = "<CHANGEME>"
//            driverClassName = "org.postgresql.Driver"
//            dialect = org.hibernate.dialect.PostgresPlusDialect
//            url = "jdbc:postgresql://localhost/apollo-production-chado"
//            properties {
//                // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
//                jmxEnabled = true
//                initialSize = 5
//                maxActive = 50
//                minIdle = 5
//                maxIdle = 25
//                maxWait = 10000
//                maxAge = 10 * 60000
//                timeBetweenEvictionRunsMillis = 5000
//                minEvictableIdleTimeMillis = 60000
//                validationQuery = "SELECT 1"
//                validationQueryTimeout = 3
//                validationInterval = 15000
//                testOnBorrow = true
//                testWhileIdle = true
//                testOnReturn = false
//                jdbcInterceptors = "ConnectionState"
//                defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
//            }
//        }
    }
}

//apollo {
//    gff3.source = "MyOrg" // also for GPAD export
//    only_owners_delete = true
//    common_data_directory = "/opt/temporary/apollo"
//    count_annotations = false
//    store_orig_id = false
//    fa_to_twobit_exe = "/usr/local/bin/faToTwoBit" // get from // https://genome.ucsc.edu/goldenPath/help/blatSpec.html
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
//
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
//         tag = "1.16.5-release"
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
