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
// not quite default apollo settings
apollo {
  default_minimum_intron_size = 1
  history_size = 0
  overlapper_class = "org.bbop.apollo.sequence.OrfOverlapper"
  track_name_comparator = "/config/track_name_comparator.js"
  use_cds_for_new_transcripts = true
  user_pure_memory_store = true
  translation_table = "/config/translation_tables/ncbi_1_translation_table.txt"
  is_partial_translation_allowed = false // unused so far
  get_translation_code = 1
  only_owners_delete = false
  sequence_search_tools = [
    blat_nuc: [
      search_exe: "/usr/local/bin/blat",
      search_class: "org.bbop.apollo.sequence.search.blat.BlatCommandLineNucleotideToNucleotide",
      name: "Blat nucleotide",
      params: ""
    ],
    blat_prot: [
      search_exe: "/usr/local/bin/blat",
      search_class: "org.bbop.apollo.sequence.search.blat.BlatCommandLineProteinToNucleotide",
      name: "Blat protein",
      params: ""
    ]
  ]    
      


  splice_donor_sites = [ "GT" ]
  splice_acceptor_sites = [ "AG"]
  gff3.source= "." 
  bootstrap = false

  info_editor = {
    feature_types = "default"
    attributes = true
    dbxrefs = true
    pubmed_ids = true
    go_ids = true
    comments = true
  }
}

jbrowse {
   git {
       url= "https://github.com/NAL-i5K/jbrowse"
//       url= "https://github.com/GMOD/jbrowse"
//        branch = "master"
//	  tag = "1.0.0"
	  tag = "e9a005cf86a40ad4b2a4aaebcbf914a866ff7f3b"
//	  tag = "maint/1.12.5-apollo"
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
//           branch = "color_second_level"
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

