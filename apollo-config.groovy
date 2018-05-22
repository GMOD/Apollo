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

// Uncomment to make changes
//
jbrowse {
   git {
       url= "https://github.com/NAL-i5K/jbrowse"
//        branch = "master"
//	  tag = "1.0.0"
	  tag = "e9a005cf86a40ad4b2a4aaebcbf914a866ff7f3b"
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
       ColorByType {
           git = "https://github.com/NAL-i5K/ColorByType"
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

