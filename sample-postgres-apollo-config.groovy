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
        dataSource.dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
        dataSource.username = "<CHANGEME>"
        dataSource.password = "<CHANGEME>"
        dataSource.driverClassName = "org.postgresql.Driver"
        dataSource.dialect = org.hibernate.dialect.PostgresPlusDialect
        dataSource.url = "jdbc:postgresql://localhost/apollo"
    }
    test {
        dataSource.dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
        dataSource.username = "<CHANGEME>"
        dataSource.password = "<CHANGEME>"
        dataSource.driverClassName = "org.postgresql.Driver"
        dataSource.dialect = "org.bbop.apollo.ImprovedPostgresDialect"
        dataSource.url = "jdbc:postgresql://localhost/apollo-test"
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
    }
}

