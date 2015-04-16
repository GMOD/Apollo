## WebApollo Configuration

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Configure.md">On GitHub</a>

WebApollo 2.0 has a basic configuration that is mostly configured using the web interface or through simple changes that override parameters in Config.groovy.

To override the defaults set in Config.groovy copy a sample-apollo-XXX.groovy file to "apollo-config.groovy" in the same folder. 

You can override any of the below configurations by putting the exact configuration between grails{  } as in the sample-apollo-config.groovy below:

    grails{
        apollo.get_translation_code =1 
        apollo{
             use_cds_for_new_transcripts = true
             default_minimum_intron_size = 1
            get_translation_code =1  // identical to the above statement
        }
    }
    
* The H2 is the default if no config is copied.   Only for development or running for a single user.
* [H2 sample file](https://github.com/GMOD/Apollo/blob/master/sample-h2-apollo-config.groovy)     
* [Postgres sample file](https://github.com/GMOD/Apollo/blob/master/sample-postgres-apollo-config.groovy)
* [MySQL sample file](https://github.com/GMOD/Apollo/blob/master/sample-mysql-apollo-config.groovy)

   
Note: Configuration options may change, moving into the web interface / database from the configuration file. 

### Main configuration


    // default apollo settings
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

        // TODO: should come from config or via preferences database
        splice_donor_sites = [ "GT"]
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


This compact representation ports the Web Apollo 1.0 config.xml parameters



### Database configuration


You can choose from using H2, Postgres, or MySQL by default. Each has a file called `sample-h2-apollo-config.groovy` or
`sample-postgres-apollo-config.groovy` that is designed to be renamed to apollo-config.groovy before running `apollo
deploy`. This will be used for the main database configuration. It can be configured in test, development, and
production modes, and the sample files show some examples of other parameters that are commonly used.



### Canned comments

Todo


### Search tools

As mentioned previously, Web Apollo allows the user to specify sequence search tools and alignment parameters. The tool blat [1] is commonly used and can be easily configured as follows.


    sequence_search_tools {
        blat_nuc {
            exe = "/usr/local/bin/blat"
            name = "Blat nucleotide"
            params = ""
        }
        blat_prot {
            exe = "/usr/local/bin/blat"
            name = "Blat protein"
            params = ""
        }
    }

Note that when you are setting up individual instances/organisms, you can specify different Blat databases via the web.


### Supported annotation types

Many configurations will require you to define which annotation types the configuration will apply to. WebApollo supports the following "higher level" types (from the Sequence Ontology):

* sequence:gene
* sequence:pseudogene
* sequence:transcript
* sequence:mRNA
* sequence:tRNA
* sequence:snRNA
* sequence:snoRNA
* sequence:ncRNA
* sequence:rRNA
* sequence:miRNA
* sequence:repeat_region
* sequence:transposable_element



### Data adapters

#### GFF3

Todo

#### FASTA
Todo

### Upgrading existing instances

Todo


#### Upgrading existing JBrowse data stores

The JBrowse data stores have remained stable so no changes to existing JBrowse data tracks are necessary in Web Apollo 2.0.

##### Sequence alterations updating

Todo
