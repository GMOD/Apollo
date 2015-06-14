## WebApollo Configuration

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Configure.md">On GitHub</a>

Web Apollo 2.0 includes some basic configuration parameters that are specified in configuration files. The most
important parameters are the database parameters in order to get Web Apollo up and running. Other options besides the
database parameters can be configured via the config files, but note that many parameters can also be configured via the
web interface.

Note: Configuration options may change over time, as more configuration items are integrated into the web interface.


### Database configuration


You can choose from using H2, Postgres, or MySQL database configurations by default.

Each has a file called `sample-h2-apollo-config.groovy` or `sample-postgres-apollo-config.groovy` that is designed to be
renamed to apollo-config.groovy before running `apollo deploy`.

The database configurations in `apollo-config.groovy` can be used in test, development, and production modes, and the
environment will be automatically selected depending on how it is run, e.g:

* `apollo deploy` or `apollo release` for a production environment
* `apollo run-local` or `apollo debug` for a development environment
* `apollo test` for a test environment

*Note:* additional general configuration from the "Main configuration" can also be added to your apollo-config.groovy.

*Note:* to deploy on tomcat you *NEED* to have a configured `apollo-config` file copied from one of the samples.

### Main configuration

The main configuration settings for Apollo are stored in Config.groovy, but you can override settings in your
`apollo-config.groovy` file (i.e. the same file that contains your database parameters). Here are the defaults that are
defined in the Config.groovy file:


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
        sequence_search_tools {
            blat_nuc {
                search_exe = "/usr/local/bin/blat"
                search_class = "org.bbop.apollo.sequence.search.blat.BlatCommandLineNucleotideToNucleotide"
                name = "Blat nucleotide"
                params = ""
            }
            blat_prot {
                search_exe = "/usr/local/bin/blat"
                search_class = "org.bbop.apollo.sequence.search.blat.BlatCommandLineProteinToNucleotide"
                name = "Blat protein"
                params = ""
            }
        }

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

These settings are essentially the same familiar parameters from a config.xml file from previous Web Apollo versions.
The defaults are generally sufficient, but as noted above, you can override any particular parameter in your
apollo-config.groovy file, e.g. you can add override configuration any given parameter as follows:

    grails {
        apollo.get_translation_code = 1 
        apollo {
             use_cds_for_new_transcripts = true
             default_minimum_intron_size = 1
             get_translation_code = 1  // identical to the dot notation
        }
    }

 

### Canned comments

Todo


### Search tools

As shown in the Main configuration, Web Apollo allows the user to specify sequence search tools. The tool UCSC BLAT is
commonly used and can be easily configured via the config file, with the general parameters given as follows:

    sequence_search_tools {
        blat_nuc {
            search_exe = "/usr/local/bin/blat"
            search_class = "org.bbop.apollo.sequence.search.blat.BlatCommandLineNucleotideToNucleotide"
            name = "Blat nucleotide"
            params = ""
        }
        blat_prot {
            search_exe = "/usr/local/bin/blat"
            search_class = "org.bbop.apollo.sequence.search.blat.BlatCommandLineProteinToNucleotide"
            name = "Blat protein"
            params = ""
            // tmp_dir = /custom/tmp/dir
        }
    }


Note: Any arbitrary search tool can be specified using this syntax, i.e. the blat_nuc and blat_prot are just recommended
defaults. You could have your own section with multiple different parameters, or even code your own search_class if it
implements the SequenceSearchTool interface. Also note: the tmp_dir is normally a system predefined folder, so it is not
normally necessary to define it.

### Supported annotation types

Many configurations will require you to define which annotation types the configuration will apply to. Web Apollo
supports the following "higher level" types (from the Sequence Ontology):

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


### Apache / Nginx Configuration

Often time admins will put Apache or Nginx in front of a servlet container (e.g., Tomcat, Jetty).  This is not necessary, but it is a very standard configuration.  

One thing to consider with this proxy setup is the websocket calls. We use the SockJS library, which will downgrade to long-polling if web sockets are not available, but since websockets are preferable, it helps to take some extra steps to ensure that the websocket calls are proxied or forwarded in some way too.


#### Apache Proxy 

The most simple setup on apache is a reverse proxy. Note that a reverse proxy _does not_ use ProxyPass On. Here is the most basic configuration:
    
    ProxyPass  /apollo http://localhost:8080/apollo
    ProxyPassReverse  /apollo http://localhost:8080/apollo
    ProxyPassReverseCookiePath  / http://localhost:8080/apollo
    
This setup will use AJAX long-polling unless websockets are also configured to be proxied. To setup the proxy for websockets, you can use mod_proxy_wstunnel (available for httpd 2.4):  http://httpd.apache.org/docs/2.4/mod/mod_proxy_wstunnel.html

First load the module or use a2enmod enable it (on ubuntu / debian):

    LoadModule proxy_wstunnel_module libexec/apache2/mod_proxy_wstunnel.so


Then in your server config, i.e. httpd.conf, add extra ProxyPass calls for the websocket "endpoint" called /apollo/stomp

    
    ProxyPass /apollo/stomp  ws://localhost:8080/apollo/stomp
    ProxyPassReverse /apollo/stomp ws://localhost:8080/apollo/stomp

    

#### Nginx Proxy (from version 1.4 on)

Your setup may vary, but setting the upgrade headers can be used for the websocket configuration http://nginx.org/en/docs/http/websocket.html

    map $http_upgrade $connection_upgrade {
            default upgrade;
            ''      close;
    }
    
    
    server {
        # Main
        listen   80;
        server_name  myserver;
        
        # http://nginx.org/en/docs/http/websocket.html
        location /ApolloSever {
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
            proxy_pass      http://127.0.0.1:8080;
        }
    }


### Data adapters

#### GFF3

Todo

#### FASTA

Todo

### Upgrading existing instances

Todo


#### Upgrading existing JBrowse data stores

It is not necessary to upgrade the JBrowse data tracks to use Web Apollo 2.0, you can just point to existing data directories from your previous instances from the Organism panel.


