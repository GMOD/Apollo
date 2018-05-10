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
            username = "webapollo"
            password = "web2apollo"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgresPlusDialect
            url = "jdbc:postgresql://localhost/webapollo_2"
        }
   }
    production {
        dataSource{
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            username = "webapollo"
            password = "web2apollo"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgresPlusDialect
            url = "jdbc:postgresql://localhost/webapollo_2"
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
// environments {
//     development {
//         // sample config to turn on debug logging in development e.g. for apollo run-local
//         log4j.main = {
//             debug "grails.app"
//         }
//         // sample config to edit apollo specific configs in development mode
//         apollo {
//             gff3.source = "testing"
//         }
//         dataSource{
//             dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
//             username = "<CHANGEME>"
//             password = "<CHANGEME>"
//             driverClassName = "org.postgresql.Driver"
//             dialect = org.hibernate.dialect.PostgresPlusDialect
//             url = "jdbc:postgresql://localhost/apollo"
//         }
// //        dataSource_chado{
// //            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
// //            username = "<CHANGEME>"
// //            password = "<CHANGEME>"
// //            driverClassName = "org.postgresql.Driver"
// //            dialect = org.hibernate.dialect.PostgresPlusDialect
// //            url = "jdbc:postgresql://localhost/apollo-chado"
// //        }
//     }
//     test {
//         dataSource{
//             dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
//             username = "<CHANGEME>"
//             password = "<CHANGEME>"
//             driverClassName = "org.postgresql.Driver"
// //        dialect = org.hibernate.dialect.PostgresPlusDialect
//             dialect = "org.bbop.apollo.ImprovedPostgresDialect"
//             url = "jdbc:postgresql://localhost/apollo-test"
//         }
// //        dataSource_chado{
// //            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
// //            username = "<CHANGEME>"
// //            password = "<CHANGEME>"
// //            driverClassName = "org.postgresql.Driver"
// ////        dialect = org.hibernate.dialect.PostgresPlusDialect
// //            dialect = "org.bbop.apollo.ImprovedPostgresDialect"
// //            url = "jdbc:postgresql://localhost/apollo-test-chado"
// //        }
//     }
//     production {
//         dataSource{
//             dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
//             username = "<CHANGEME>"
//             password = "<CHANGEME>"
//             driverClassName = "org.postgresql.Driver"
//             dialect = org.hibernate.dialect.PostgresPlusDialect
//             url = "jdbc:postgresql://localhost/apollo-production"
//             properties {
//                 // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
//                 jmxEnabled = true
//                 initialSize = 5
//                 maxActive = 50
//                 minIdle = 5
//                 maxIdle = 25
//                 maxWait = 10000
//                 maxAge = 10 * 60000
//                 timeBetweenEvictionRunsMillis = 5000
//                 minEvictableIdleTimeMillis = 60000
//                 validationQuery = "SELECT 1"
//                 validationQueryTimeout = 3
//                 validationInterval = 15000
//                 testOnBorrow = true
//                 testWhileIdle = true
//                 testOnReturn = false
//                 jdbcInterceptors = "ConnectionState"
//                 defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
//             }
//         }
// //        dataSource_chado{
// //            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
// //            username = "<CHANGEME>"
// //            password = "<CHANGEME>"
// //            driverClassName = "org.postgresql.Driver"
// //            dialect = org.hibernate.dialect.PostgresPlusDialect
// //            url = "jdbc:postgresql://localhost/apollo-production-chado"
// //            properties {
// //                // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
// //                jmxEnabled = true
// //                initialSize = 5
// //                maxActive = 50
// //                minIdle = 5
// //                maxIdle = 25
// //                maxWait = 10000
// //                maxAge = 10 * 60000
// //                timeBetweenEvictionRunsMillis = 5000
// //                minEvictableIdleTimeMillis = 60000
// //                validationQuery = "SELECT 1"
// //                validationQueryTimeout = 3
// //                validationInterval = 15000
// //                testOnBorrow = true
// //                testWhileIdle = true
// //                testOnReturn = false
// //                jdbcInterceptors = "ConnectionState"
// //                defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
// //            }
// //        }
//     }
// }

// Uncomment to make changes
//
jbrowse {
   git {
       url= "https://github.com/NAL-i5K/jbrowse"
        branch = "master"
//url= "https://github.com/NAL-i5K/jbrowse#master"
//       branch = "master#db01bc0c65f339d7fdacfd13973937b41b109c84"
       //tag = "974a414338807964d66bf8ed9503207e30cea293"
       alwaysPull = true
       alwaysRecheck = true

	// Warning: We are still testing the performance of NeatFeatures plugins in combination with Apollo.
	// We advise caution if enabling these plugins with Apollo until this process is finalized.
   }
   plugins {
       WebApollo{
           included = true
       }
    //    NeatHTMLFeatures{
    //        included = true
    //    }
    //    NeatCanvasFeatures{
    //        included = true
    //    }
       RegexSequenceSearch{
           included = true
       }
       HideTrackLabels{
           included = true
       }
    //    MyVariantInfo {
    //        git = 'https://github.com/GMOD/myvariantviewer'
    //        branch = 'master'
    //        alwaysRecheck = "true"
    //        alwaysPull = "true"
    //    }
    //    SashimiPlot {
    //        git = 'https://github.com/cmdcolin/sashimiplot'
    //        branch = 'master'
    //        alwaysPull = "true"
    //    }
       NAL_CSS {
       	   git = "https://github.com/NAL-i5K/NAL_CSS"
       	   branch = "master"
           alwaysPull = true
     	   alwaysRecheck = true
       }
//        Header {
//           git = 'https://github.com/NAL-i5K/workspace_header_footer'
//               branch = 'master'
//           alwaysPull = true
//           alwaysRecheck = true
//       }
   }
}

