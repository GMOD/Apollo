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
//
//	// Warning: We are still testing the performance of NeatFeatures plugins in combination with Apollo.
//	// We advise caution if enabling these plugins with Apollo until this process is finalized.
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
//    }
//}

