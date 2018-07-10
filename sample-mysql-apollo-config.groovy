
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
            driverClassName = "com.mysql.jdbc.Driver"
            dialect = org.hibernate.dialect.MySQL5InnoDBDialect
            url = "jdbc:mysql://localhost/apollo"
        }
    }
    test {
        dataSource{
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "<CHANGEME>"
            password = "<CHANGEME>"
            driverClassName = "com.mysql.jdbc.Driver"
            dialect = org.hibernate.dialect.MySQL5InnoDBDialect
            url = "jdbc:mysql://localhost/apollo-test"
        }
    }
    production {
        dataSource{
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "<CHANGEME>"
            password = "<CHANGEME>"
            driverClassName = "com.mysql.jdbc.Driver"
            dialect = org.hibernate.dialect.MySQL5InnoDBDialect
            url = "jdbc:mysql://localhost/apollo-production"
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
//        tag = "maint/1.12.5-apollo"
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

