dataSource {
    dbCreate = System.getenv("WEBAPOLLO_DB_ACTION") ?: "update" // one of 'create', 'create-drop', 'update', 'validate', ''
    username = System.getenv("WEBAPOLLO_DB_USERNAME")
    password = System.getenv("WEBAPOLLO_DB_PASSWORD")

    driverClassName = System.getenv("WEBAPOLLO_DB_DRIVER") ?: "org.postgresql.Driver"
    dialect = System.getenv("WEBAPOLLO_DB_DIALECT") ?: "org.hibernate.dialect.PostgresPlusDialect"
    url = System.getenv("WEBAPOLLO_DB_URI")
}

environments {
    development {
    }
    test {
        dataSource {
            dbCreate = System.getenv("WEBAPOLLO_DB_ACTION") ?: "create-drop"
            dialect = System.getenv("WEBAPOLLO_DB_DIALECT") ?: "org.bbop.apollo.ImprovedPostgresDialect"
        }
    }
    production {
        dataSource {
            properties {
                // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
                jmxEnabled = System.getenv("WEBAPOLLO_JMX_ENABLED").equals("true")
                initialSize                   = System.getenv("WEBAPOLLO_SERVER_INITIAL_SIZE")             ? System.getenv("WEBAPOLLO_SERVER_INITIAL_SIZE").toInteger()             : 5
                maxActive                     = System.getenv("WEBAPOLLO_SERVER_MAX_ACTRIVE")              ? System.getenv("WEBAPOLLO_SERVER_MAX_ACTRIVE").toInteger()              : 50
                minIdle                       = System.getenv("WEBAPOLLO_SERVER_MIN_IDLE")                 ? System.getenv("WEBAPOLLO_SERVER_MIN_IDLE").toInteger()                 : 5
                maxIdle                       = System.getenv("WEBAPOLLO_SERVER_MAX_IDLE")                 ? System.getenv("WEBAPOLLO_SERVER_MAX_IDLE").toInteger()                 : 25
                maxWait                       = System.getenv("WEBAPOLLO_SERVER_MAX_WAIT")                 ? System.getenv("WEBAPOLLO_SERVER_MAX_WAIT").toInteger()                 : 10000
                maxAge                        = System.getenv("WEBAPOLLO_SERVER_MAX_AGE")                  ? System.getenv("WEBAPOLLO_SERVER_MAX_AGE").toInteger()                  : 10 * 60000
                timeBetweenEvictionRunsMillis = System.getenv("WEBAPOLLO_SERVER_EVICTION_DELAY")           ? System.getenv("WEBAPOLLO_SERVER_EVICTION_DELAY").toInteger()           : 5000
                minEvictableIdleTimeMillis    = System.getenv("WEBAPOLLO_SERVER_MIN_EVICTABLE_IDLE")       ? System.getenv("WEBAPOLLO_SERVER_MIN_EVICTABLE_IDLE").toInteger()       : 60000
                validationQuery               = System.getenv("WEBAPOLLO_SERVER_VALIDATION_QUERY")         ? System.getenv("WEBAPOLLO_SERVER_VALIDATION_QUERY").toInteger()         : "SELECT 1"
                validationQueryTimeout        = System.getenv("WEBAPOLLO_SERVER_VALIDATION_QUERY_TIMEOUT") ? System.getenv("WEBAPOLLO_SERVER_VALIDATION_QUERY_TIMEOUT").toInteger() : 3
                validationInterval            = System.getenv("WEBAPOLLO_SERVER_VALIDATION_INTERVAL")      ? System.getenv("WEBAPOLLO_SERVER_VALIDATION_INTERVAL").toInteger()      : 15000
                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = false
                jdbcInterceptors = "ConnectionState"
                defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
            }
        }
    }
}

