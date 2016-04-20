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
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
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
            //Please specify the appropriate file path, otherwise /tmp/prodDb will be used.
            //url = "jdbc:h2:/opt/apollo/h2/prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            url = "jdbc:h2:/tmp/prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
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

// Uncomment to make changes
//
//jbrowse {
//    git {
//        url= "https://github.com/GMOD/jbrowse"
//        tag = "1.12.2-release"
////        branch = "master"
//        alwaysPull = true
//        alwaysRecheck = true
//    }
//    plugins {
//        WebApollo{
//            included = true
//        }
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

