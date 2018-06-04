dataSource {
    pooled = true
    jmxExport = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
//hibernate {
//    cache.use_second_level_cache=true
//    cache.use_query_cache=true
//    cache.provider_class='org.hibernate.cache.EhCacheProvider'
//    cache.region.factory_class = 'grails.plugin.cache.ehcache.hibernate.BeanEhcacheRegionFactory4' // For Hibernate 4.0 and higher
//
//}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
//    cache.region.factory_class = 'org.hibernate.cache.SingletonEhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
    flush.mode = 'manual' // OSIV session flush mode outside of transactional context
}

// environment specific settings
environments {
    development {
        dataSource {
            //dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            // url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            url = "jdbc:h2:AnnotationDatabase:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
//        dataSource_chado{
//            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
//            url = "jdbc:h2:mem:chadoDevDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
//        }
    }
    test {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            dialect = "org.bbop.apollo.ImprovedH2Dialect"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
//        dataSource_chado {
//            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
//            dialect = "org.bbop.apollo.ImprovedH2Dialect"
//            url = "jdbc:h2:mem:chadoTestDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
//        }
    }
    production {
        dataSource {
            dbCreate = "update"
            // NOTE: Not to be used for production.  Please see:  http://genomearchitect.readthedocs.io/en/latest/Configure/
            // you should copy over sample-XXX-config.groovy
//            url = "jdbc:h2:/tmp/prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
            url = ""
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
//        dataSource_chado {
//            dbCreate = "update"
//            // NOTE: Not to be used for production.  Please see:  http://genomearchitect.readthedocs.io/en/latest/Configure/
//            // you should copy over sample-XXX-config.groovy
////            url = "jdbc:h2:/tmp/chadoProdDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
//            url = ""
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

