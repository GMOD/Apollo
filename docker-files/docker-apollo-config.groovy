println "WEBAPOLLO environment"
System.getenv().each {
    if (it.key.contains("APOLLO") || it.key.contains("CHADO")) {
        println it.key + " " + it.value
    }
}

environments {
    development {
    }
    test {
    }
    production {
        dataSource {
            dbCreate = "update"
            username = System.getenv("WEBAPOLLO_DB_USERNAME") ?: "apollo"
            password = System.getenv("WEBAPOLLO_DB_PASSWORD") ?: "apollo"

            driverClassName = "org.postgresql.Driver"
            dialect = "org.hibernate.dialect.PostgresPlusDialect"
            url = "jdbc:postgresql://${System.getenv("WEBAPOLLO_DB_HOST") ?: "127.0.0.1"}/${System.getenv("WEBAPOLLO_DB_NAME") ?: "apollo"}"

            properties {
                // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
                jmxEnabled = false
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
        if (System.getenv("WEBAPOLLO_USE_CHADO") == "true") {
            dataSource_chado {
                dbCreate = "update"
                username = System.getenv("CHADO_DB_USERNAME") ?: "apollo"
                password = System.getenv("CHADO_DB_PASSWORD") ?: "apollo"

                driverClassName = "org.postgresql.Driver"
                dialect = "org.hibernate.dialect.PostgresPlusDialect"

                url = "jdbc:postgresql://${System.getenv("CHADO_DB_HOST") ?: "127.0.0.1"}/${System.getenv("CHADO_DB_NAME") ?: "chado"}"

                properties {
                    // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
                    jmxEnabled = false
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
}

apollo {
    common_data_directory = System.getenv("WEBAPOLLO_COMMON_DATA") ? System.getenv("WEBAPOLLO_COMMON_DATA") : "/data/temporary/apollo_data"
    default_minimum_intron_size = System.getenv("WEBAPOLLO_MINIMUM_INTRON_SIZE") ? System.getenv("WEBAPOLLO_MINIMUM_INTRON_SIZE").toInteger() : 1
    history_size = System.getenv("WEBAPOLLO_HISTORY_SIZE") ? System.getenv("WEBAPOLLO_HISTORY_SIZE").toInteger() : 0
    overlapper_class = System.getenv("WEBAPOLLO_OVERLAPPER_CLASS") ?: "org.bbop.apollo.sequence.OrfOverlapper"
    use_cds_for_new_transcripts = System.getenv("WEBAPOLLO_CDS_FOR_NEW_TRANSCRIPTS").equals("true")
    // will default to false
    feature_has_dbxrefs = System.getenv("WEBAPOLLO_FEATURE_HAS_DBXREFS") ?: true
    feature_has_attributes = System.getenv("WEBAPOLLO_FEATURE_HAS_ATTRS") ?: true
    feature_has_pubmed_ids = System.getenv("WEBAPOLLO_FEATURE_HAS_PUBMED") ?: true
    feature_has_go_ids = System.getenv("WEBAPOLLO_FEATURE_HAS_GO") ?: true
    feature_has_comments = System.getenv("WEBAPOLLO_FEATURE_HAS_COMMENTS") ?: true
    feature_has_status = System.getenv("WEBAPOLLO_FEATURE_HAS_STATUS") ?: true
    translation_table = "/config/translation_tables/ncbi_" + (System.getenv("WEBAPOLLO_TRANSLATION_TABLE") ?: "1") + "_translation_table.txt"
    get_translation_code = System.getenv("WEBAPOLLO_TRANSLATION_TABLE") ? System.getenv("WEBAPOLLO_TRANSLATION_TABLE").toInteger() : 1

    // TODO: should come from config or via preferences database
    splice_donor_sites = System.getenv("WEBAPOLLO_SPLICE_DONOR_SITES") ? System.getenv("WEBAPOLLO_SPLICE_DONOR_SITES").split(",") : ["GT"]
    splice_acceptor_sites = System.getenv("WEBAPOLLO_SPLICE_ACCEPTOR_SITES") ? System.getenv("WEBAPOLLO_SPLICE_ACCEPTOR_SITES").split(",") : ["AG"]
    gff3.source = System.getenv("WEBAPOLLO_GFF3_SOURCE") ?: "."

    google_analytics = System.getenv("WEBAPOLLO_GOOGLE_ANALYTICS_ID") ?: ["UA-62921593-1"]

    admin {
        username = System.getenv("APOLLO_ADMIN_EMAIL") ?: "admin@local.host"
        password = System.getenv("APOLLO_ADMIN_PASSWORD") ?: "password"
        firstName = System.getenv("APOLLO_ADMIN_FIRST_NAME") ?: "Ad"
        lastName = System.getenv("APOLLO_ADMIN_LAST_NAME") ?: "min"
    }
    authentications = [
        [
            "name":"Remote User Authenticator",
            "className":"remoteUserAuthenticatorService",
            "active": System.getenv("WEBAPOLLO_REMOTE_USER_AUTH") ?: false
        ],
        [
            "name":"Username Password Authenticator",
            "className":"usernamePasswordAuthenticatorService",
            "active": System.getenv("WEBAPOLLO_USER_PASSWORD_AUTH") ?: true
        ]
    ]
}

jbrowse {
    git {
        url = "https://github.com/GMOD/jbrowse"
        branch = "1.16.4-release"
    }
    plugins {
        WebApollo {
            included = true
        }
        NeatHTMLFeatures {
            included = true
        }
        NeatCanvasFeatures {
            included = true
        }
        RegexSequenceSearch {
            included = true
        }
        HideTrackLabels {
            included = true
        }
//        GCContent{
//            git = 'https://github.com/cmdcolin/GCContent'
//            branch = 'master'
//            alwaysRecheck = "true"
//            alwaysPull = "true"
//        }
    }
}
