// TODO: get docker working as well . . .

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

apollo {
    default_minimum_intron_size = System.getenv("WEBAPOLLO_MINIMUM_INTRON_SIZE") ? System.getenv("WEBAPOLLO_MINIMUM_INTRON_SIZE").toInteger() : 1
    history_size = System.getenv("WEBAPOLLO_HISTORY_SIZE") ? System.getenv("WEBAPOLLO_HISTORY_SIZE").toInteger() : 0
    overlapper_class = System.getenv("WEBAPOLLO_OVERLAPPER_CLASS") ?: "org.bbop.apollo.sequence.OrfOverlapper"
    use_cds_for_new_transcripts = System.getenv("WEBAPOLLO_CDS_FOR_NEW_TRANSCRIPTS").equals("true")
    feature_has_dbxrefs = System.getenv("WEBAPOLLO_FEATURE_HAS_DBXREFS").equals("true")
    feature_has_attributes = System.getenv("WEBAPOLLO_FEATURE_HAS_ATTRS").equals("true")
    feature_has_pubmed_ids = System.getenv("WEBAPOLLO_FEATURE_HAS_PUBMED").equals("true")
    feature_has_go_ids = System.getenv("WEBAPOLLO_FEATURE_HAS_GO").equals("true")
    feature_has_comments = System.getenv("WEBAPOLLO_FEATURE_HAS_COMMENTS").equals("true")
    feature_has_status = System.getenv("WEBAPOLLO_FEATURE_HAS_STATUS").equals("true")
    translation_table = "/config/translation_tables/ncbi_" + (System.getenv("WEBAPOLLO_TRANSLATION_TABLE") ?: "1") + "_translation_table.txt"
    get_translation_code = System.getenv("WEBAPOLLO_TRANSLATION_TABLE") ? System.getenv("WEBAPOLLO_TRANSLATION_TABLE").toInteger() : 1

    // TODO: should come from config or via preferences database
    splice_donor_sites = System.getenv("WEBAPOLLO_SPLICE_DONOR_SITES") ? System.getenv("WEBAPOLLO_SPLICE_DONOR_SITES").split(",") : ["GT"]
    splice_acceptor_sites = System.getenv("WEBAPOLLO_SPLICE_ACCEPTOR_SITES") ? System.getenv("WEBAPOLLO_SPLICE_ACCEPTOR_SITES").split(",") : ["AG"]
    gff3.source = System.getenv("WEBAPOLLO_GFF3_SOURCE") ?: "."

    google_analytics = System.getenv("WEBAPOLLO_GOOGLE_ANALYTICS_ID") ?: ["UA-62921593-1"]

    admin{
        username = System.getenv("APOLLO_ADMIN_EMAIL") ?: "admin@local.host"
        password = System.getenv("APOLLO_ADMIN_PASSWORD") ?: "password"
        firstName = System.getenv("APOLLO_ADMIN_FIRST_NAME") ?: "Ad"
        lastName = System.getenv("APOLLO_ADMIN_LAST_NAME") ?: "min"
    }
}

jbrowse {
    git {
        url = System.getenv("WEBAPOLLO_JBROWSE_GIT_URL") ? System.getenv("WEBAPOLLO_JBROWSE_GIT_URL") : "https://github.com/GMOD/jbrowse"
        tag = System.getenv("WEBAPOLLO_JBROWSE_GIT_TAG") ? System.getenv("WEBAPOLLO_JBROWSE_GIT_TAG") : "1.12.2-apollo"
        alwaysPull = System.getenv("WEBAPOLLO_JBROWSE_GIT_ALWAYS_PULL").equals("true")
        alwaysRecheck = System.getenv("WEBAPOLLO_JBROWSE_GIT_ALWAYS_RECHECK").equals("true")
    }
    plugins {
        WebApollo{
            included = true
        }
        NeatHTMLFeatures{
            included = System.getenv("WEBAPOLLO_JBROWSE_PLUGIN_NEATHTML").equals("true")
        }
        NeatCanvasFeatures{
            included = System.getenv("WEBAPOLLO_JBROWSE_PLUGIN_NEATCANVAS").equals("true")
        }
        RegexSequenceSearch{
            included = System.getenv("WEBAPOLLO_JBROWSE_PLUGIN_REGEXSEARCH").equals("true")
        }
        HideTrackLabels{
            included = System.getenv("WEBAPOLLO_JBROWSE_PLUGIN_HIDELABELS").equals("true")
        }
        // TODO
    }
}
