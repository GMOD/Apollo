## WebApollo Configuration

WebApollo's basic configuration is simply giving it parameters in the config.properties file but more extensive configuration can be added to the XML configuration files as well.

The available configuration files include

- config.properies - basic configuration options for startup
- config.xml - general configuration options for running webapollo
- blat_config.xml - basic blat plugin setup for searching blat in the browser
- hibernate.xml - basic hibernate database parameters for chado export
- mapping.xml - GBOL mapping to sequence ontology terms

### Main configuration

The main configuration is config.xml. Let’s take a look at the file.

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<server_configuration>

    <!-- mapping configuration for GBOL data structures -->
    <gbol_mapping>/config/mapping.xml</gbol_mapping>

    <!-- directory where JE database will be created -->
    <datastore_directory>ENTER_DATASTORE_DIRECTORY_HERE</datastore_directory>

    <!-- minimum size for introns created -->
    <default_minimum_intron_size>1</default_minimum_intron_size>

    <!-- size of history for each feature - setting to 0 means unlimited history -->
    <history_size>0</history_size>

    <!-- overlapping strategy for adding transcripts to genes -->
    <overlapper_class>org.bbop.apollo.web.overlap.OrfOverlapper</overlapper_class>

    <!-- javascript file for comparing track names (refseqs) (used for sorting in selection table) -->
    <track_name_comparator>/config/track_name_comparator.js</track_name_comparator>

    <!-- whether to use an existing CDS when creating new transcripts -->
    <use_cds_for_new_transcripts>true</use_cds_for_new_transcripts>

    <!-- set to false to use hybrid disk/memory store which provides a little slower performance
    but uses a lot less memory - great for annotation rich genomes -->
    <use_pure_memory_store>true</use_pure_memory_store>

    <!-- user authentication/permission configuration -->
    <user>

        <!-- database configuration -->
        <database>

            <!-- driver for user database -->
            <driver>org.postgresql.Driver</driver>

            <!-- JDBC URL for user database -->
            <url>jdbc:postgresql:web_apollo_users</url>

            <!-- username for user database -->
            <username>web_apollo_users_admin</username>

            <!-- password for user database -->
            <password>web_apollo_users_admin</password>

        </database>

        <!-- class for generating user authentication page
        (login page) -->
        <authentication_class>org.bbop.apollo.web.user.localdb.LocalDbUserAuthentication</authentication_class>

    </user>

    <tracks>

        <!-- annotation track name the current convention is to append
        the genomic region id to the the name of the annotation track
        e.g., if the annotation track is called "Annotations" and the
        genomic region is chr2L, the track name will be
        "Annotations-chr2L".-->
        <annotation_track_name>Annotations</annotation_track_name>

        <!-- CV term for the genomic sequences - should be in the form
        of "CV:term".  This applies to all sequences -->
        <!--<sequence_type>ENTER_CVTERM_FOR_SEQUENCE</sequence_type>-->
        <sequence_type>sequence:supercontig</sequence_type>

        <!-- path to file containing translation table.
        optional - defaults to NCBI translation table 1 if
        absent -->
        <translation_table>/config/translation_tables/ncbi_1_translation_table.txt</translation_table>

        <!-- splice acceptor and donor sites. Multiple entries may be
        added to allow multiple accepted sites.
        optional - defaults to GT for donor and AG for acceptor
        if absent -->
        <splice_sites>
            <donor_site>GT</donor_site>
            <acceptor_site>AG</acceptor_site>
        </splice_sites>

    </tracks>

    <!-- path to file containing canned comments XML -->
    <canned_comments>/config/canned_comments.xml</canned_comments>

    <!-- configuration for what to display in the annotation info editor.
    Sections can be commented out to not be displayed or uncommented
    to make them active -->
    <annotation_info_editor>

        <!-- grouping for the configuration.  The "feature_types" attribute takes a list of
        SO terms (comma separated) to apply this configuration to
        (e.g., feature_types="sequence:transcript,sequence:mRNA" will make it so the group
        configuration will only apply to features of type "sequence:transcript" or "sequence:mRNA").
        A value of "default" will make this the default configuration for any types not explicitly
        defined in other groups.  You can have any many groups as you'd like -->
        <annotation_info_editor_group feature_types="default">

            <!-- display status section.  The text for each <status_flag>
            element will be displayed as a radio button in the status
            section, in the same order -->
            <!--
            <status>
                <status_flag>Approved</status_flag>
                <status_flag>Needs review</status_flag>
            </status>
            -->
            
            <!-- display generic attributes section -->
            <attributes />

            <!-- display dbxrefs section -->
            <dbxrefs />

            <!-- display PubMed IDs section -->
            <pubmed_ids />

            <!-- display GO IDs section -->
            <go_ids />

            <!-- display comments section -->
            <comments />

        </annotation_info_editor_group>

    </annotation_info_editor>

    <!-- tools to be used for sequence searching.  This is optional.
    If this is not setup, WebApollo will not have sequence search support -->
    <sequence_search_tools>

        <!-- one <sequence_search_tool> element per tool -->
        <sequence_search_tool>

            <!-- display name for the search tool -->
            <key>BLAT nucleotide</key>

            <!-- class for handling search -->
            <class>org.bbop.apollo.tools.seq.search.blat.BlatCommandLineNucleotideToNucleotide</class>

            <!-- configuration for search tool -->
            <config>/config/blat_config.xml</config>

        </sequence_search_tool>

        <sequence_search_tool>

            <!-- display name for the search tool -->
            <key>BLAT protein</key>

            <!-- class for handling search -->
            <class>org.bbop.apollo.tools.seq.search.blat.BlatCommandLineProteinToNucleotide</class>

            <!-- configuration for search tool -->
            <config>/config/blat_config.xml</config>

        </sequence_search_tool>

    </sequence_search_tools>

    <!-- data adapters for writing annotation data to different formats.
    These will be used to dynamically generate data adapters within
    WebApollo.  It contains either <data_adapter> or <data_adapter_group> elements.
    <data_adapter_group> will allow grouping adapters together and will provide a
    submenu for those adapters in WebApollo. This is optional.  -->
    <data_adapters>

        <!-- one <data_adapter> element per data adapter -->
        <data_adapter>

            <!-- display name for data adapter -->
            <key>GFF3</key>

            <!-- class for data adapter plugin -->
            <class>org.bbop.apollo.web.dataadapter.gff3.Gff3DataAdapter</class>

            <!-- required permission for using data adapter
            available options are: read, write, publish -->
            <permission>read</permission>

            <!-- configuration file for data adapter -->
            <config>/config/gff3_config.xml</config>

            <!-- options to be passed to data adapter -->
            <options>output=file&amp;format=gzip</options>

        </data_adapter>

        <data_adapter>

            <!-- display name for data adapter -->
            <key>Chado</key>

            <!-- class for data adapter plugin -->
            <class>org.bbop.apollo.web.dataadapter.chado.ChadoDataAdapter</class>

            <!-- required permission for using data adapter
            available options are: read, write, publish -->
            <permission>publish</permission>

            <!-- configuration file for data adapter -->
            <config>/config/chado_config.xml</config>

            <!-- options to be passed to data adapter -->
            <options>display_features=false</options>

        </data_adapter>

        <!-- group the <data_adapter> children elements together -->
        <data_adapter_group>

            <!-- display name for adapter group -->
            <key>FASTA</key>

            <!-- required permission for using data adapter group
            available options are: read, write, publish -->
            <permission>read</permission>

            <!-- one child <data_adapter> for each data adapter in the group -->
            <data_adapter>

                <!-- display name for data adapter -->
                <key>peptide</key>

                <!-- class for data adapter plugin -->
                <class>org.bbop.apollo.web.dataadapter.fasta.FastaDataAdapter</class>
                
                <!-- required permission for using data adapter
                available options are: read, write, publish -->
                <permission>read</permission>

                <!-- configuration file for data adapter -->
                <config>/config/fasta_config.xml</config>

                <!-- options to be passed to data adapter -->
                <options>output=file&amp;format=gzip&amp;seqType=peptide</options>

            </data_adapter>

            <data_adapter>

                <!-- display name for data adapter -->
                <key>cDNA</key>

                <!-- class for data adapter plugin -->
                <class>org.bbop.apollo.web.dataadapter.fasta.FastaDataAdapter</class>
                
                <!-- required permission for using data adapter
                available options are: read, write, publish -->
                <permission>read</permission>

                <!-- configuration file for data adapter -->
                <config>/config/fasta_config.xml</config>

                <!-- options to be passed to data adapter -->
                <options>output=file&amp;format=gzip&amp;seqType=cdna</options>

            </data_adapter>

            <data_adapter>

                <!-- display name for data adapter -->
                <key>CDS</key>

                <!-- class for data adapter plugin -->
                <class>org.bbop.apollo.web.dataadapter.fasta.FastaDataAdapter</class>
                
                <!-- required permission for using data adapter
                available options are: read, write, publish -->
                <permission>read</permission>

                <!-- configuration file for data adapter -->
                <config>/config/fasta_config.xml</config>

                <!-- options to be passed to data adapter -->
                <options>output=file&amp;format=gzip&amp;seqType=cds</options>

            </data_adapter>

        </data_adapter_group>

    </data_adapters>

</server_configuration>
```

Let’s look through each element in more detail with values filled in.

``` xml
<!-- mapping configuration for GBOL data structures -->
<gbol_mapping>/config/mapping.xml</gbol_mapping>
```

File that contains type mappings used by the underlying data model. It’s best not to change the default option.

``` xml
<!-- directory where JE database will be created -->
<datastore_directory>WEB_APOLLO_DATA_DIR</datastore_directory>
```

Directory where user generated annotations will be stored. The data is stored using Berkeley DB.

``` xml
<!-- minimum size for introns created -->
<default_minimum_intron_size>1</default_minimum_intron_size>
```

Minimum length of intron to be created when using the “Make intron” operation. The operation will try to make the shortest intron that’s at least as long as this parameter. So if you set it to a value of “40”, then all calculated introns will be at least of length 40.

``` xml
<!-- size of history for each feature - setting to 0 means unlimited history -->
<history_size>0</history_size>
```

The size of your history stack, meaning how many “Undo/Redo” steps you can do. The larger the number, the larger the storage space needed. Setting it to “0” makes it to that there’s no limit.

``` xml
<!-- overlapping strategy for adding transcripts to genes -->
<overlapper_class>org.bbop.apollo.web.overlap.OrfOverlapper</overlapper_class>
```

Defines the strategy to be used for deciding whether overlapping transcripts should be considered splice variants to the same gene. This points to a Java class implementing the `org.bbop.apollo.overlap.Overlapper` interface. This allows you to create your own custom overlapping strategy should the need arise. Currently available options are:

-   `org.bbop.apollo.web.overlap.NoOverlapper`
    -   No transcripts should be considered splice variants, regardless of overlap.
-   `org.bbop.apollo.web.overlap.SimpleOverlapper`
    -   Any overlapping of transcripts will cause them to be part of the same gene
-   `org.bbop.apollo.web.overlap.OrfOverlapper`
    -   Only transcripts that overlap within the coding region and within frame are considered part of the same gene

``` xml
<!-- javascript file for comparing track names (refseqs) (used for sorting in selection table) -->
<track_name_comparator>/config/track_name_comparator.js</track_name_comparator>
```

Defines how to compare genomic sequence names for sorting purposes in the genomic region selection list. Points to a javascript file. You can implement your logic to allow whatever sorting you’d like for your own organism. This doesn't make much of a difference in our case since we're only dealing with one genomic region. The default behavior is to sort names lexicographically.

``` xml
<!-- whether to use an existing CDS when creating new transcripts -->
<use_cds_for_new_transcripts>true</use_cds_for_new_transcripts>
```

Tells Web Apollo whether to use an existing CDS when creating a new transcript (otherwise it computes the longest ORF). This can be useful when gene predictors suggest a CDS that's not the longest ORF and you want to use that instead. This is only applicable when using features that have a CDS associated with them.

``` xml
<!-- set to false to use hybrid disk/memory store which provides a little slower performance
but uses a lot less memory - great for annotation rich genomes -->
<use_pure_memory_store>true</use_pure_memory_store>
```

Defines whether the internal data store is purely a memory one or a hybrid memory/disk store. The memory store provides faster performance at the cost of more memory. The hybrid store provides a little slower performance but uses a lot less memory, so it's a good option for annotation rich genomes. Set to `true` to use the memory store and `false` to use the hybrid one.

Let’s take look at the `user` element, which handles configuration for user authentication and permission handling.

``` xml
<!-- user authentication/permission configuration -->
<user>

    <!-- database configuration -->
    <database>

        <!-- driver for user database -->
        <driver>org.postgresql.Driver</driver>

        <!-- JDBC URL for user database -->
        <url>ENTER_USER_DATABASE_JDBC_URL</url>

        <!-- username for user database -->
        <username>ENTER_USER_DATABASE_USERNAME</username>

        <!-- password for user database -->
        <password>ENTER_USER_DATABASE_PASSWORD</password>

    </database>

    <!-- class for generating user authentication page (login page) -->
    <authentication_class>org.bbop.apollo.web.user.localdb.LocalDbUserAuthentication</authentication_class>

</user>
```

Let’s first look at the `database` element that defines the database that will handle user permissions (which we created previously).

``` xml
<!-- driver for user database -->
<driver>org.postgresql.Driver</driver>
```

This should point the JDBC driver for communicating with the database. We’re using a PostgreSQL driver since that’s the database we’re using for user permission management.

``` xml
<!-- JDBC URL for user database -->
<url>jdbc:postgresql://localhost/web_apollo_users</url>
```

JDBC URL to the user permission database. We'll use `jdbc:postgresql://localhost/web_apollo_users` since the database is running in the same server as the annotation editing engine and we named the database `web_apollo_users`.

``` xml
<!-- username for user database -->
<username>web_apollo_users_admin</username>
```

User name that has read/write access to the user database. The user with access to the user database has the user name `web_apollo_users_admin`.

``` xml
<!-- password for user database -->
<password>web_apollo_users_admin</password>
```

Password to access user database. The user with access to the user database has the password </tt>web\_apollo\_users\_admin</tt>.

Now let’s look at the other elements in the `user` element.

``` xml
<!-- class for generating user authentication page (login page) -->
<authentication_class>org.bbop.apollo.web.user.localdb.LocalDbUserAuthentication</authentication_class>
```

Defines how user authentication is handled. This points to a class implementing the `org.bbop.apollo.web.user.UserAuthentication` interface. This allows you to implement any type of authentication you’d like (e.g., LDAP). Currently available options are:

-   `org.bbop.apollo.web.user.localdb.LocalDbUserAuthentication`
    -   Uses the user permission database to also store authentication information, meaning it stores user passwords in the database
-   `org.bbop.apollo.web.user.browserid.BrowserIdUserAuthentication`
    -   Uses Mozilla’s [BrowserID](https://browserid.org) service for authentication. This has the benefits of offloading all authentication security to Mozilla and allows one account to have access to multiple resources (as long as they have BrowserID support). Being that the service is provided through Mozilla, it will require users to create a BrowserID account

Now let’s look at the configuration for accessing the annotation tracks for the genomic sequences.

``` xml
<tracks>

    <!-- path to JBrowse refSeqs.json file -->
    <refseqs>ENTER_PATH_TO_REFSEQS_JSON_FILE</refseqs>

    <!-- annotation track name the current convention is to append
        the genomic region id to the the name of the annotation track
        e.g., if the annotation track is called "Annotations" and the
        genomic region is chr2L, the track name will be
        "Annotations-chr2L".-->
    <annotation_track_name>Annotations</annotation_track_name>

    <!-- organism being annotated -->
    <organism>ENTER_ORGANISM</organism>

    <!-- CV term for the genomic sequences - should be in the form
        of "CV:term".  This applies to all sequences -->
    <sequence_type>ENTER_CVTERM_FOR_SEQUENCE</sequence_type>

    <!-- path to file containing translation table.
        optional - defaults to NCBI translation table 1 if absent -->
    <translation_table>/config/translation_tables/ncbi_1_translation_table.txt</translation_table>

    <!-- splice acceptor and donor sites. Multiple entries may be
        added to allow multiple accepted sites.
        optional - defaults to GT for donor and AG for acceptor
        if absent -->
    <splice_sites>
        <donor_site>GT</donor_site>
        <acceptor_site>AG</acceptor_site>
    </splice_sites>

</tracks>
```

Let’s look at each element individually.

``` xml
<!-- path to JBrowse refSeqs.json file -->
<refseqs>TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse/data/seq/refSeqs.json</refseqs>
```

Location where the `refSeqs.json` file resides, which is created from the data generation pipeline (see the [data generation](#Data_generation "wikilink") section). By default, the JBrowse data needs to reside in `TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse/data`. If you want the data to reside elsewhere, you’ll need to do configure your servlet container to handle the appropriate alias to `jbrowse/data` or symlink the `data` directory to somewhere else. Web Apollo is pre-configured to allow symlinks.

<font color="red">IMPORTANT</font>: In the previous versions of Web Apollo (2013-05-16 and prior), this element pointed to the symlink created from the data generation pipeline. The current pipeline no longer creates the symlink, so you need to point to the actual file itself (hence `jbrowse/data/<font color="red">seq</font>/refSeqs.json` as opposed to `jbrowse/data/refSeqs.json` in the previous versions. If you're accessing data generated from a previous version of Web Apollo, you'll still need to point to the symlink.

``` xml
<annotation_track_name>Annotations</annotation_track_name>
```

Name of the annotation track. Leave it as the default value of `Annotations`.

``` xml
<!-- organism being annotated -->
<organism>Pythium ultimum</organism>
```

Scientific name of the organism being annotated (genus and species). We're annotating `Pythium ultimum`.

``` xml
<!-- CV term for the genomic sequences - should be in the form
    of "CV:term".  This applies to all sequences -->
<sequence_type>sequence:contig</sequence_type>
```

The type for the genomic sequences. Should be in the form of `CV:term`. Our genomic sequences are of the type `sequence:contig`.

``` xml
<!-- path to file containing translation table.
    optional - defaults to NCBI translation table 1 if absent -->
<translation_table>/config/translation_tables/ncbi_1_translation_table.txt</translation_table>
```

File that contains the codon translation table. This is optional and defaults to NCBI translation table 1 if absent. See the [translation tables](#Translation_tables "wikilink") section for details on which tables are available and how to customize your own table.

``` xml
<!-- splice acceptor and donor sites. Multiple entries may be
    added to allow multiple accepted sites.
    optional - defaults to GT for donor and AG for acceptor
    if absent -->
<splice_sites>
    <donor_site>GT</donor_site>
    <acceptor_site>AG</acceptor_site>
</splice_sites>
```

Defines what the accepted donor and acceptor splice sites are. This will determine whether the client displays a warning on splice sites (if the splice site sequence doesn't match what's defined here, then it flags the splice site). You can add multiple `<donor_site>` and `<acceptor_site>` elements if your organism should support multiple values. This is optional and defaults to `GT` for donor and `AG` for acceptor sites.

``` xml
<!-- path to file containing canned comments XML -->
<canned_comments>/config/canned_comments.xml</canned_comments>
```

File that contains canned comments (predefined comments that will be available from a pull-down menu when creating comments). It’s best not to change the default option. See the [canned comments](#Canned_comments "wikilink") section for details on configuring canned comments.

``` xml
<!-- configuration for what to display in the annotation info editor.
Sections can be commented out to not be displayed or uncommented
to make them active -->
<annotation_info_editor>

    <!-- grouping for the configuration.  The "feature_types" attribute takes a list of
    SO terms (comma separated) to apply this configuration to
    (e.g., feature_types="sequence:transcript,sequence:mRNA" will make it so the group
    configuration will only apply to features of type "sequence:transcript" or "sequence:mRNA").
    A value of "default" will make this the default configuration for any types not explicitly
    defined in other groups.  You can have any many groups as you'd like -->
    <annotation_info_editor_group feature_types="default">

        <!-- display status section.  The text for each <status_flag>
        element will be displayed as a radio button in the status
        section, in the same order -->
        <!--
        <status>
            <status_flag>Approved</status_flag>
            <status_flag>Needs review</status_flag>
        </status>
        -->

        <!-- display generic attributes section -->
        <attributes />

        <!-- display dbxrefs section -->
        <dbxrefs />

        <!-- display PubMed IDs section -->
        <pubmed_ids />

        <!-- display GO IDs section -->
        <go_ids />

        <!-- display comments section -->
        <comments />

    </annotation_info_editor_group>

</annotation_info_editor>
```

Here's the configuration on what to display in the annotation info editor. It will always display `Name`, `Symbol`, and `Description` but the rest is optional. This allows you to make the editor more compact if you're not interested in editing certain metadata. Let's look at the options in more detail.

``` xml
<!-- grouping for the configuration.  The "feature_types" attribute takes a list of
SO terms (comma separated) to apply this configuration to
(e.g., feature_types="sequence:transcript,sequence:mRNA" will make it so the group
configuration will only apply to features of type "sequence:transcript" or "sequence:mRNA").
A value of "default" will make this the default configuration for any types not explicitly
defined in other groups.  You can have any many groups as you'd like -->
<annotation_info_editor_group feature_types="default">
    ...
</annotation_info_editor_group>
```

Each configuration is grouped by annotation type. This allows you to have different options on what's displayed for specified types. The `feature_types` attribute defines which types this group will apply to. `feature_types` takes a list of SO terms (comma separated), such as `"sequence:transcript,sequence:mRNA"`, which will apply this configuration to annotations of type `sequence:transcript` and `sequence:mRNA`. Alternatively, you can set the value to `"default"` which will become the default configuration for any types not explicitly defined in other groups. You can have any many groups as you'd like. All [supported annotation types](#Supported_annotation_types "wikilink") can be used.

Next, let's look at each item to configure in each group.

``` xml
<!-- display status section.  The text for each <status_flag>
    element will be displayed as a radio button in the status
    section, in the same order -->
<status>
    <status_flag>Approved</status_flag>
    <status_flag>Needs review</status_flag>
</status>
```

Allows selecting the status for a particular annotation. The value for `<status_flag>` is arbitrary (you can enter any text) and you can add as many as you'd like, but you need to at least have one (they'll show up as selectable buttons in the editor).

``` xml
<!-- display generic attributes section -->
<attributes />
```

Allows editing of generic attributes (tag/value pairs). Think non-reserved GFF3 tags for column 9.

``` xml
<!-- display dbxrefs section -->
<dbxrefs />
```

Allows editing of database cross references.

``` xml
<!-- display PubMed IDs section -->
<pubmed_ids />
```

Allows editing of PubMed IDs (for associating an annotation with a publication).

``` xml
<!-- display GO IDs section -->
<go_ids />
```

Allows editing of Gene Ontology terms (for associating an annotation to a particular function).

``` xml
<!-- display comments section -->
<comments />
```

Allows editing of comments for annotations.

``` xml
<!-- tools to be used for sequence searching.  This is optional.
    If this is not setup, WebApollo will not have sequence search support -->
<sequence_search_tools>

    <!-- one <sequence_search_tool> element per tool -->
    <sequence_search_tool>

        <!-- display name for the search tool -->
        <key>BLAT nucleotide</key>

        <!-- class for handling search -->
        <class>org.bbop.apollo.tools.seq.search.blat.BlatCommandLineNucleotideToNucleotide</class>

        <!-- configuration for search tool -->
        <config>/config/blat_config.xml</config>

    </sequence_search_tool>

    <sequence_search_tool>

        <!-- display name for the search tool -->
        <key>BLAT protein</key>

        <!-- class for handling search -->
        <class>org.bbop.apollo.tools.seq.search.blat.BlatCommandLineProteinToNucleotide</class>

        <!-- configuration for search tool -->
        <config>/config/blat_config.xml</config>

    </sequence_search_tool>

</sequence_search_tools>
```

Here’s the configuration for sequence search tools (allows searching your genomic sequences). Web Apollo does not implement any search algorithms, but instead relies on different tools and resources to handle searching (this provides much more flexible search options). This is optional. If it’s not configured, Web Apollo will not have sequence search support. You'll need one `sequence_search_tool` element per search tool. Let's look at the element in more detail.

``` xml
<!-- display name for the search tool -->
<key>BLAT nucleotide</key>
```

This is a string that will be used for the display name for the search tool, in the pull down menu that provides search selection for the user.

``` xml
<!-- class for handling search -->
<class>org.bbop.apollo.tools.seq.search.blat.BlatCommandLineNucleotideToNucleotide</class>
```

Should point to the class that will handle the search request. Searching is handled by classes that implement the `org.bbop.apollo.tools.seq.search.SequenceSearchTool` interface. This allows you to add support for your own favorite search tools (or resources). We currently only have support for command line Blat, in the following flavors:

-   `org.bbop.apollo.tools.seq.search.blat.BlatCommandLineNucleotideToNucleotide`
    -   Blat search for a nucleotide query against a nucleotide database
-   `org.bbop.apollo.tools.seq.search.blat.BlatCommandLineProteinToNucleotide`
    -   Blat search for a protein query against a nucleotide database

``` xml
<!-- configuration for search tool -->
<config>/config/blat_config.xml</config>
```

File that contains the configuration for the searching plugin chosen. If you’re using Blat, you should not change this. If you’re using your own plugin, you’ll want to point this to the right configuration file (which will be dependent on your plugin). See the [Blat](#Blat "wikilink") section for details on configuring Web Apollo to use Blat.

``` xml
<!-- data adapters for writing annotation data to different formats.
These will be used to dynamically generate data adapters within
WebApollo.  It contains either <data_adapter> or <data_adapter_group> elements.
<data_adapter_group> will allow grouping adapters together and will provide a
submenu for those adapters in WebApollo. This is optional.  -->
<data_adapters>

    <!-- one <data_adapter> element per data adapter -->
    <data_adapter>

        <!-- display name for data adapter -->
        <key>GFF3</key>

        <!-- class for data adapter plugin -->
        <class>org.bbop.apollo.web.dataadapter.gff3.Gff3DataAdapter</class>

        <!-- required permission for using data adapter
        available options are: read, write, publish -->
        <permission>read</permission>

        <!-- configuration file for data adapter -->
        <config>/config/gff3_config.xml</config>

        <!-- options to be passed to data adapter -->
        <options>output=file&amp;format=gzip</options>

    </data_adapter>

    <data_adapter>

        <!-- display name for data adapter -->
        <key>Chado</key>

        <!-- class for data adapter plugin -->
        <class>org.bbop.apollo.web.dataadapter.chado.ChadoDataAdapter</class>

        <!-- required permission for using data adapter
        available options are: read, write, publish -->
        <permission>publish</permission>

        <!-- configuration file for data adapter -->
        <config>/config/chado_config.xml</config>

        <!-- options to be passed to data adapter -->
        <options>display_features=false</options>

    </data_adapter>

    <!-- group the <data_adapter> children elements together -->
    <data_adapter_group>

        <!-- display name for adapter group -->
        <key>FASTA</key>

        <!-- required permission for using data adapter group
        available options are: read, write, publish -->
        <permission>read</permission>

        <!-- one child <data_adapter> for each data adapter in the group -->
        <data_adapter>

            <!-- display name for data adapter -->
            <key>peptide</key>

            <!-- class for data adapter plugin -->
            <class>org.bbop.apollo.web.dataadapter.fasta.FastaDataAdapter</class>
                
            <!-- required permission for using data adapter
            available options are: read, write, publish -->
            <permission>read</permission>

            <!-- configuration file for data adapter -->
            <config>/config/fasta_config.xml</config>

            <!-- options to be passed to data adapter -->
            <options>output=file&amp;format=gzip&amp;seqType=peptide</options>

        </data_adapter>

        <data_adapter>

            <!-- display name for data adapter -->
            <key>cDNA</key>

            <!-- class for data adapter plugin -->
            <class>org.bbop.apollo.web.dataadapter.fasta.FastaDataAdapter</class>
                
            <!-- required permission for using data adapter
            available options are: read, write, publish -->
            <permission>read</permission>

            <!-- configuration file for data adapter -->
            <config>/config/fasta_config.xml</config>

            <!-- options to be passed to data adapter -->
            <options>output=file&amp;format=gzip&amp;seqType=cdna</options>

        </data_adapter>

        <data_adapter>

            <!-- display name for data adapter -->
            <key>CDS</key>

            <!-- class for data adapter plugin -->
            <class>org.bbop.apollo.web.dataadapter.fasta.FastaDataAdapter</class>
                
            <!-- required permission for using data adapter
            available options are: read, write, publish -->
            <permission>read</permission>

            <!-- configuration file for data adapter -->
            <config>/config/fasta_config.xml</config>

            <!-- options to be passed to data adapter -->
            <options>output=file&amp;format=gzip&amp;seqType=cds</options>

        </data_adapter>

    </data_adapter_group>

</data_adapters>
```

Here’s the configuration for data adapters (allows writing annotations to different formats). This is optional. If it’s not configured, Web Apollo will not have data writing support. You'll need one `&lt;data_adapter&gt;` element per data adapter. You can group data adapters by placing each `&lt;data_adapter&gt;` inside a `&lt;data_adapter_group&gt;` element. Let's look at the `&lt;data_adapter&gt;` element in more detail.

``` xml
<!-- display name for data adapter -->
<key>GFF3</key>
```

This is a string that will be used for the data adapter name, in the dynamically generated data adapters list for the user.

``` xml
<!-- class for data adapter plugin -->
<class>org.bbop.apollo.web.dataadapter.gff3.Gff3DataAdapter</class>
```

Should point to the class that will handle the write request. Writing is handled by classes that implement the `org.bbop.apollo.web.dataadapter.DataAdapter` interface. This allows you to add support for writing to different formats. We currently only have support for:

-   `org.bbop.apollo.web.dataadapter.gff3.Gff3DataAdapter`
    -   GFF3 (see the [GFF3](#GFF3 "wikilink") section for details on this adapter)
-   `org.bbop.apollo.web.dataadapter.chado.ChadoDataAdapter`
    -   Chado (see the [Chado](#Chado "wikilink") section for details on this adapter)

``` xml
<!-- required permission for using data adapter
    available options are: read, write, publish -->
<permission>publish</permission>
```

Required user permission for accessing this data adapter. If the user does not have the required permission, it will not be available in the list of data adapters. Available permissions are `read`, `write`, and `publish`.

``` xml
<!-- configuration for data adapter -->
<config>/config/gff3_config.xml</config>
```

File that contains the configuration for the data adapter plugin chosen.

``` xml
<!-- options to be passed to data adapter -->
<options>output=file&amp;format=gzip</options>
```

Options to be passed to the data adapter. These are dependent on the data adapter.

Next, let's look at the `&lt;data_adapter_group&gt;` element:

``` xml
<!-- display name for adapter group -->
<key>FASTA</key>
```

This is a string that will be used for the data adapter submenu name.

<permission>read</permission> Required user permission for accessing this data adapter group. If the user does not have the required permission, it will not be available in the list of data adapters. Available permissions are `read`, `write`, and `publish`.

### Translation tables

Web Apollo has support for alternate translation tables. For your convenience, Web Apollo comes packaged with the current NCBI translation tables. They reside in the `config/translation_tables` directory in your installation (`TOMCAT_WEBAPPS_DIR/WebApollo/config/translation_tables`). They're all named `ncbi_#_translation_table.txt` where `#` represents the NCBI translation table number (for example, for ciliates, you'd use `ncbi_6_translation_table.txt`).

You can also customize your own translation table. The format is tab delimited, with each entry containing either 2 or 3 columns. The 3rd column is only used in the cases of start and stop codons. You only need to put entries for codons that differ from the standard translation table (\#1). The first column has the codon triplet and the second has the IUPAC single letter representation for the translated amino acid. The stop codon should be represented as `*` (asterisk).

``` text
TAA Q
```

As mentioned previously, you'll only need the 3rd column for start and stop codons. To denote a codon as a start codon, put in `start` in the third column. For example, if we wanted to assign `GTG` as a start codon, we'd enter:

``` text
GTG V   start
```

For stop codons, if we enter an IUPAC single letter representation for the amino acid in the 3rd column, we're denoting that amino acid to be used in the case of a readthrough stop codon. For example, to use pyrrolysine, we'd enter:

``` text
TAG *   O
```

If you write your own customized translation table, make sure to update the `<translation_table>` element in your configuration to your customized file.

### Canned comments

You can configure a set of predefined comments that will be available for users when adding comments through a dropdown menu. The configuration is stored in `/usr/local/tomcat/tomcat7/webapps/WebApollo/config/canned_comments.xml`. Let’s take a look at the configuration file.

``` xml
<?xml version="1.0" encoding="UTF-8"?>

<canned_comments>
    <!-- one <comment> element per comment.
    it must contain either the attribute "feature_type" that defines
    the type of feature this comment will apply to or the attribute "feature_types"
    that defines a list (comma separated) of types of features this comment will
    apply to.
    types must be be in the form of "CV:term" (e.g., "sequence:gene")

    <comment feature_type="sequence:gene">This is a comment for sequence:gene</comment>
    or
    <comment feature_types="sequence:tRNA,sequence:ncRNA">This is a comment for both sequence:tRNA and sequence:ncRNA</comment>
    -->
</canned_comments>
```

You’ll need one `<comment>` element for each predefined comment. The element needs to have either a `feature_type` attribute in the form of `CV:term` that this comment applies to or a `feature_types` attribute, a comma separated list of types this comment will apply to, where each type is also in the form of `CV:term`. Let’s make a few comments for feature of type `sequence:gene` and `sequence:transcript`, `sequence:mRNA`:

``` xml
<comment feature_type="sequence:gene">This is a comment for a gene</comment>
<comment feature_type="sequence:gene">This is another comment for a gene</comment>
<comment feature_types="sequence:transcript,sequence:mRNA">This is a comment for both a transcript or mRNA</comment>
```

All [supported annotation types](#Supported_annotation_types "wikilink") can be used.

### Search tools

As mentioned previously, Web Apollo makes use of tools for sequence searching rather than employing its own search algorithm. The only currently supported tool is command line Blat.

#### Blat

You’ll need to have Blat installed and a search database with your genomic sequences available to make use of this feature. You can get documentation on the Blat command line suite of tools at [BLAT Suite Program Specifications and User Guide](http://genome.ucsc.edu/goldenPath/help/blatSpec.html) and get information on setting up the tool in the official [BLAT FAQ](http://genome.ucsc.edu/FAQ/FAQblat.html#blat3). The configuration is stored in `TOMCAT_WEBAPPS_DIR/WebApollo/config/blat_config.xml`. Let’s take a look at the configuration file:

``` xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- configuration file for setting up command line Blat support -->

<blat_config>

    <!-- path to Blat binary →
    <blat_bin>ENTER_PATH_TO_BLAT_BINARY</blat_bin>

    <!-- path to where to put temporary data -->
    <tmp_dir>ENTER_PATH_FOR_TEMPORARY_DATA</tmp_dir>

    <!-- path to Blat database -->
    <database>ENTER_PATH_TO_BLAT_DATABASE</database>

    <!-- any Blat options (directly passed to Blat) e.g., -minMatch -->
    <blat_options>ENTER_ANY_BLAT_OPTIONS</blat_options>

    <!-- true to remove temporary data path after search (set to false for debugging purposes) -->
    <remove_tmp_dir>true</remove_tmp_dir>

</blat_config>
```

Let’s look at each element with values filled in.

``` xml
<!-- path to Blat binary -->
<blat_bin>BLAT_DIR/blat</blat_bin>
```

We need to point to the location where the Blat binary resides. For this guide, we'll assume Blat in installed in `/usr/local/bin`.

``` xml
<!-- path to where to put temporary data -->
<tmp_dir>BLAT_TMP_DIR</tmp_dir>
```

We need to point to the location where to store temporary files to be used in the Blat search. It can be set to whatever location you’d like.

``` xml
<!-- path to Blat database -->
<database>BLAT_DATABASE</database>
```

We need to point to the location of the search database to be used by Blat. See the Blat documentation for more information on generation search databases.

``` xml
<!-- any Blat options (directly passed to Blat) e.g., -minMatch -->
<blat_options>-minScore=100 -minIdentity=60</blat_options>
```

Here we can configure any extra options to used by Blat. These options are passed verbatim to the program. In this example, we’re passing the `-minScore` parameter with a minimum score of `100` and the `-minIdentity` parameter with a value of `60` (60% identity). See the Blat documentation for information of all available options.

``` xml
<!-- true to remove temporary data path after search (set to false for debugging purposes) -->
<remove_tmp_dir>true</remove_tmp_dir>
```

Whether to delete the temporary files generated for the BLAT search. Set it to `false` to not delete the files after the search, which is useful for debugging why your search may have failed or returned no results.

### Data adapters

#### GFF3

The GFF3 data adapter will allow exporting the current annotations as a GFF3 file. You can get more information about the GFF3 format at [The Sequence Ontology GFF3 page](http://www.sequenceontology.org/gff3.shtml). The configuration is stored in `TOMCAT_WEBAPPS_DIR/WebApollo/config/gff3_config.xml`. Let’s take a look at the configuration file:

``` xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- configuration file for GFF3 data adapter -->

<gff3_config>

    <!-- path to where to put generated GFF3 file.  This path is 
        relative path that will be where you deployed your 
        instance (so that it's accessible from HTTP download requests) -->
    <tmp_dir>tmp</tmp_dir>

    <!-- value to use in the source column (column 2) of the generated
        GFF3 file. -->
    <source>.</source>

    <!-- which metadata to export as an attribute - optional.
        Default is to export everything except owner, date_creation, and date_last_modified -->
    <!--
    <metadata_to_export>
        <metadata type="name" />
        <metadata type="symbol" />
        <metadata type="description" />
        <metadata type="status" />
        <metadata type="dbxrefs" />
        <metadata type="attributes" />
        <metadata type="comments" />
        <metadata type="owner" />
        <metadata type="date_creation" />
        <metadata type="date_last_modified" />
    </metadata_to_export>
    -->

    <!-- whether to export underlying genomic sequence - optional.
    Defaults to true -->
    <export_source_genomic_sequence>true</export_source_genomic_sequence>

</gff3_config>
```

``` xml
<tmp_dir>tmp</tmp_dir>
```

This is the root directory where the GFF3 files will be generated. The actual GFF3 files will be in subdirectories that are generated to prevent collisions from concurrent requests. This directory is relative to `TOMCAT_WEBAPPS_DIR/WebApollo`. This is done to allow the generated GFF3 to be accessible from HTTP requests.

``` xml
<!-- value to use in the source column (column 2) of the generated
    GFF3 file. -->
<source>.</source>
```

This is what to put as the source (column 2) in the generated GFF3 file. You can change the value to anything you'd like.

``` xml
<!-- which metadata to export as an attribute - optional.
    Default is to export everything except owner, date_creation, and date_last_modified -->
<metadata_to_export>
    <metadata type="name" />
    <metadata type="symbol" />
    <metadata type="description" />
    <metadata type="status" />
    <metadata type="dbxrefs" />
    <metadata type="attributes" />
    <metadata type="comments" />
    <metadata type="owner" />
    <metadata type="date_creation" />
    <metadata type="date_last_modified" />
</metadata_to_export>
```

This defines which metadata to export in the GFF3 (in column 9). This configuration is optional. The default is to export everything except owner, date\_creation, and date\_last\_modified. You need to define one `&lt;metadata<&gt;` element with the appropriate `type` attribute per metadata type you want to export. Available types are:

-   name
-   symbol
-   description
-   status
-   dbxrefs
-   attributes
-   comments
-   owner
-   date\_creation
-   date\_last\_modified

``` xml
<!-- whether to export underlying genomic sequence - optional.
Defaults to true -->
<export_source_genomic_sequence>true</export_source_genomic_sequence>
```

Determines whether to export the underlying genomic sequence as FASTA attached to the GFF3 file. Set to `false` to disable it. Defaults to `true`.

Note that the generated files will reside in that directory indefinitely to allow users to download them. You'll need to eventually remove those files to prevent the file system from cluttering up. There's a script that will traverse the directory and remove any files that are older than a provided time and cleanup directories as they become empty. It's recommended to setup this script as a `cron` job that runs hourly to remove any files older than an hour (should provide plenty of time for users to download those files). The script is in `WEB_APOLLO_DIR/tools/cleanup/remove_temporary_files.sh`.

`$ WEB_APOLLO_DIR/tools/cleanup/remove_temporary_files.sh -d TOMCAT_WEBAPPS_DIR/WebApollo/tmp -m 60`

#### Chado

The Chado data adapter will allow writing the current annotations to a Chado database. You can get more information about the Chado at [GMOD Chado page](http://gmod.org/wiki/Chado). The configuration is stored in `TOMCAT_WEBAPPS_DIR/WebApollo/config/chado_config.xml`. Let’s take a look at the configuration file:

``` xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- configuration file for Chado data adapter -->

<chado_config>

    <!-- Hibernate configuration file for accessing Chado database -->
    <hibernate_config>/config/hibernate.xml</hibernate_config>

</chado_config>
```

There's only one element to be configured:

``` xml
<hibernate_config>/config/hibernate.xml</hibernate_config>
```

This points to the Hibernate configuration for accessing the Chado database. Hibernate provides an ORM (Object Relational Mapping) for relational databases. This is used to access the Chado database. The Hibernate configuration is stored in `TOMCAT_WEBAPPS_DIR/WebApollo/config/hibernate.xml`. It is quite large (as it contains a lot of mapping resources), so let's take a look at the parts of the configuration file that are of interest (near the top of the file):

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE hibernate-configuration PUBLIC
        "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
        "http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd">
<hibernate-configuration>
    <session-factory name="SessionFactory">
        <property name="hibernate.connection.driver_class">org.postgresql.Driver</property>
        <property name="hibernate.connection.url">ENTER_DATABASE_CONNECTION_URL</property>
        <property name="hibernate.connection.username">ENTER_USERNAME</property>
        <property name="hibernate.connection.password">ENTER_PASSWORD</property>

        ...

    </session-factory>
</hibernate-configuration>
```

Let's look at each element:

``` xml
<property name="hibernate.connection.driver_class">org.postgresql.Driver</property>
```

The database driver for the RDBMS where the Chado database exists. It will most likely be PostgreSQL (as it's the officially recommended RDBMS for Chado), in which case you should leave this at its default value.

``` xml
<property name="hibernate.connection.url">ENTER_DATABASE_CONNECTION_URL</property>
```

JDBC URL to connect to the Chado database. It will be in the form of `jdbc:$RDBMS://$SERVERNAME:$PORT/$DATABASE_NAME` where `$RDBMS` is the RDBMS used for the Chado database, `$SERVERNAME` is the server's name, `$PORT` is the database port, and `$DATABASE_NAME` is the database's name. Let's say we're connecting to a Chado database running on PostgreSQL on server `my_server`, port `5432` (PostgreSQL's default), and a database name of `my_organism`, the connection URL will look as follows: `jdbc:postgresql://my_server:5432/my_organism`.

``` xml
<property name="hibernate.connection.username">ENTER_USERNAME</property>
```

User name used to connect to the database. This user should have write privileges to the database.

``` xml
<property name="hibernate.connection.password">ENTER_PASSWORD</property>
```

Password for the provided user name.

'''Important Note for first-time Export '''

Make sure to load your chromosomes into Chado before you do an export.

To do this you export your GFF file from Apollo (using the GFF3 export also detailed in this section) and [<http://gmod.org/wiki/Load_GFF_Into_Chado>| import the GFF3 file into Chado]. Example:

``` bash
./load/bin/gmod_bulk_load_gff3.pl --gfffile ~/Amel/Amel_4.5_scaffolds.gff --dbuser USERNAME \ 
--dbpass PASSWORD --dbname CHADO_DB --organism "Apis mellifera"
```

#### FASTA

The FASTA data adapter will allow exporting the current annotations to a FASTA file. The configuration is stored in `TOMCAT_WEBAPPS_DIR/WebApollo/config/fasta_config.xml`. Let’s take a look at the configuration file:

``` xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- configuration file for FASTA data adapter -->

<fasta_config>

    <!-- path to where to put generated FASTA file.  This path is a
    relative path that will be where you deployed your WebApollo
    instance (so that it's accessible from HTTP download requests) -->
    <tmp_dir>tmp</tmp_dir>

    <!-- feature types to process when dumping FASTA sequence -->
    <feature_types>

        <!-- feature type to process - one element per type -->
        <feature_type>sequence:mRNA</feature_type>

        <feature_type>sequence:transcript</feature_type>

    </feature_types>

    <!-- which metadata to export as an attribute - optional.
    Default does not export any metadata -->
    <!--
    <metadata_to_export>
        <metadata type="name" />
        <metadata type="symbol" />
        <metadata type="description" />
        <metadata type="status" />
        <metadata type="dbxrefs" />
        <metadata type="attributes" />
        <metadata type="comments" />
        <metadata type="owner" />
        <metadata type="date_creation" />
        <metadata type="date_last_modified" />
    </metadata_to_export>
    -->

</fasta_config>
```

``` xml
<!-- path to where to put generated FASTA file.  This path is a
relative path that will be where you deployed your WebApollo
instance (so that it's accessible from HTTP download requests) -->
<tmp_dir>tmp</tmp_dir>
```

This is the root directory where the FASTA files will be generated. The actual FASTA files will be in subdirectories that are generated to prevent collisions from concurrent requests. This directory is relative to TOMCAT\_WEBAPPS\_DIR/WebApollo. This is done to allow the generated FASTA to be accessible from HTTP requests.

``` xml
<!-- feature types to process when dumping FASTA sequence -->
<feature_types>

    <!-- feature type to process - one element per type -->
    <feature_type>sequence:mRNA</feature_type>

    <feature_type>sequence:transcript</feature_type>

</feature_types>
```

This defines which annotation types should be processed when exporting the FASTA data. You'll need one `&lt;feature_type&gt;` element for each type you want to have processed. Only the defined `feature_type` elements will all be processed, so you might want to have different configuration files for processing different types of annotations (which you can point to in FASTA data adapter in the `config` element in `config.xml`). All [supported annotation types](#Supported_annotation_types "wikilink") can be used for the value of `feature_type`, with the addition of `sequence:exon`.

In `config.xml`, in the `&lt;options&gt;` element in the `&lt;data_adapter&gt;` configuration for the FASTA adapter, you'll notice that there's a `seqType` option. You can change that value to modify which type of sequence will be exported as FASTA. Available options are:

-   peptide
    -   Export the peptide sequence. This will only apply to protein coding transcripts and protein coding exons
-   cdna
    -   Export the cDNA sequence. This will only apply to transcripts and exons
-   cds
    -   Export the CDS sequence. This will only apply to protein coding transcripts and protein coding exons
-   genomic
    -   Export the genomic within the feature's boundaries. This applies to all feature types.

``` xml
<!-- which metadata to export as an attribute - optional.
Default does not export any metadata -->
<!--
<metadata_to_export>
    <metadata type="name" />
    <metadata type="symbol" />
    <metadata type="description" />
    <metadata type="status" />
    <metadata type="dbxrefs" />
    <metadata type="attributes" />
    <metadata type="comments" />
    <metadata type="owner" />
    <metadata type="date_creation" />
    <metadata type="date_last_modified" />
</metadata_to_export>
-->
```

Defines which metadata to export in the defline for each feature. The default is to not output any of the listed metadata. Uncomment to turn on this option. Note that you can remove (or comment) any `&lt;metadata&gt;` elements that you're not interested in exporting.

Note that like the GFF3 adapter, the generated files will reside in that directory indefinitely to allow users to download them. You'll need to eventually remove those files to prevent the file system from cluttering up. You can use the `remove_temporary_files.sh` script to handle the cleanup. In fact, if you configure both the GFF3 and FASTA adapters to use the same temporary directory, you'll only need to worry about cleanup from a single location. See the [GFF3](#GFF3 "wikilink") section for information about `remove_temporary_files.sh`.

Data generation
---------------

The steps for generating data (in particular static data) are mostly similar to [JBrowse](JBrowse "wikilink") data generation steps, with some extra steps required. The scripts for data generation reside in `TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse/bin`. Let's go into WebApollo's JBrowse directory.

`$ `<span class="enter">`cd TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse`</span>

It will make things easier if we make sure that the scripts in the `bin` directory are executable.

`$ `<span class="enter">`chmod 755 bin/*`</span>

As mentioned previously, the data resides in the `data` directory by default. We can symlink `JBROWSE_DATA_DIR` giving you a lot of flexibility in allowing your WebApollo instance to easily point to a new data directory.

`$ `<span class="enter">`ln -sf JBROWSE_DATA_DIR data`</span>

<font color="red">IMPORTANT</font>: If you're using data generated in previous versions of WebApollo (2013-09-04 and prior), you won't need to regenerate the data, but you will need to run the [Adding the WebApollo plugin](#Adding_the_WebApollo_plugin "wikilink") step.

### DNA track setup

The first thing we need to do before processing our evidence is to generate the reference sequence data to be used by JBrowse. We'll use the `prepare-refseqs.pl` script.

`$ `<span class="enter">`bin/prepare-refseqs.pl --fasta WEB_APOLLO_SAMPLE_DIR/scf1117875582023.fa`</span>

We now have the DNA track setup. Note that you can also use a GFF3 file containing the genomic sequence by using the `--gff` option instead of `--fasta` and point it to the GFF3 file.

### Adding the WebApollo plugin

We now need to setup the data configuration to use the WebApollo plugin. We'll use the `add-webapollo-plugin.pl` script to do so.

`$ `<span class="enter">`bin/add-webapollo-plugin.pl -i data/trackList.json`</span>

### Static data generation

Generating data from GFF3 works best by having a separate GFF3 per source type. If your GFF3 has all source types in the same file, we need to split up the GFF3. We can use the `split_gff_by_source.pl` script in `WEB_APOLLO_DIR/tools/data` to do so. We'll output the split GFF3 to some temporary directory (we'll use `WEB_APOLLO_SAMPLE_DIR/split_gff`).

`$ `<span class="enter">`mkdir -p WEB_APOLLO_SAMPLE_DIR/split_gff`</span>
`$ `<span class="enter">`WEB_APOLLO_DIR/tools/data/split_gff_by_source.pl \`
`-i WEB_APOLLO_SAMPLE_DIR/scf1117875582023.gff -d WEB_APOLLO_SAMPLE_DIR/split_gff`</span>

If we look at the contents of `WEB_APOLLO_SAMPLE_DIR/split_gff`, we can see we have the following files:

`$ `<span class="enter">`ls WEB_APOLLO_SAMPLE_DIR/split_gff`</span>
`blastn.gff  est2genome.gff  protein2genome.gff  repeatrunner.gff`
`blastx.gff  maker.gff       repeatmasker.gff    snap_masked.gff`

We need to process each file and create the appropriate tracks.

(If you've previously used JBrowse, you may know that JBrowse also has an alternative approach to generating multiple static data tracks from a GFF3 file, which uses the biodb-to-json script and a configuration file. However, WebApollo is not yet compatible with that approach)

#### GFF3 with gene/transcript/exon/CDS/polypeptide features

We'll start off with `maker.gff`. We need to handle that file a bit differently than the rest of the files since the GFF represents the features as gene, transcript, exons, and CDSs.

`$ `<span class="enter">`bin/flatfile-to-json.pl --gff WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff \`
`--arrowheadClass trellis-arrowhead --getSubfeatures \`
`--subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \`
`--className container-16px --type mRNA --trackLabel maker`</span>

Note that `brightgreen-80pct`, `darkgreen-60pct`, `container-100pct`, `container-16px`, `gray-center-20pct` are all CSS classes defined in WebApollo stylesheets that describe how to display their respective features and subfeatures. WebApollo also tries to use reasonable default CSS styles, so it is possible to omit these CSS class arguments. For example, to accept default styles for maker.gff, the above could instead be shortened to:

`$ `<span class="enter">`bin/flatfile-to-json.pl --gff WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff \`
`--getSubfeatures --type mRNA --trackLabel maker`</span>

See the [Customizing features](#Customizing_features "wikilink") section for more information on CSS styles. There are also many other configuration options for flatfile-to-json.pl, see [JBrowse data formatting](JBrowse_Configuration_Guide#Data_Formatting "wikilink") for more information.

#### GFF3 with match/match\_part features

Now we need to process the other remaining GFF3 files. The entries in those are stored as "match/match\_part", so they can all be handled in a similar fashion.

We'll start off with `blastn` as an example.

`$ `<span class="enter">`bin/flatfile-to-json.pl --gff WEB_APOLLO_SAMPLE_DIR/split_gff/blastn.gff \`
`--arrowheadClass webapollo-arrowhead --getSubfeatures \`
`--subfeatureClasses '{"match_part": "darkblue-80pct"}' \`
`--className container-10px --trackLabel blastn`</span>

Again, `container-10px` and `darkblue-80pct` are CSS class names that define how to display those elements. See the [Customizing features](#Customizing_features "wikilink") section for more information.

We need to follow the same steps for the remaining GFF3 files. It can be a bit tedious to do this for the remaining six files, so we can use a simple Bash shell script to help us out (write the script to a file and execute as shown below). Don't worry if the script doesn't make sense, you can always process each file manually on the command line:

`  `<span class="enter">`for i in $(ls WEB_APOLLO_SAMPLE_DIR/split_gff/*.gff | grep -v maker); do`
`    j=$(basename $i)`
`    j=${j/.gff/}`
`    echo "Processing $j"`
`    bin/flatfile-to-json.pl --gff $i --arrowheadClass webapollo-arrowhead \`
`    --getSubfeatures --subfeatureClasses "{\"match_part\": \"darkblue-80pct\"}" \`
`    --className container-10px --trackLabel $j`
`  done`

`$ /bin/bash myscript.sh`

</span>

#### Generate searchable name index

Once data tracks have been created, you will need to generate a searchable index of names using the generate-names.pl script:

`$ `<span class="enter">`bin/generate-names.pl`</span>

This script creates an index of sequence names and feature names in order to enable auto-completion in the navigation text box. This index is required, so if you do not wish any of the feature tracks to be indexed for auto-completion, you can instead run generate-names.pl immediately after running prepare\_refseqs.pl, but before generating other tracks.

The script can be also rerun after any additional tracks are generated if you wish feature names from that track to be added to the index (using the `--incremental` option).

<font color="red">IMPORTANT</font>: If you're running this script with a Perl version 5.10 or older, you'll need to add the `--safeMode` option. Note that running it in safe mode will be much slower.

#### BAM data

Now let's look how to configure BAM support. WebApollo has native support for BAM, so no extra processing of the data is required.

First we'll copy the BAM data into the WebApollo data directory. We'll put it in the `data/bam` directory. Keep in mind that this BAM data was randomly generated, so there's really no biological meaning to it. We only created it to show BAM support.

`$ `<span class="enter">`mkdir data/bam`</span>
`$ `<span class="enter">`cp WEB_APOLLO_SAMPLE_DIR/*.bam* data/bam`</span>

Now we need to add the BAM track.

`$ `<span class="enter">`bin/add-bam-track.pl --bam_url bam/simulated-sorted.bam \ `
`   --label simulated_bam --key "simulated BAM"`</span>

You should now have a `simulated BAM` track available.

#### BigWig data

WebApollo has native support for BigWig files (.bw), so no extra processing of the data is required.

Configuring a BigWig track is very similar to configuring a BAM track. First we'll copy the BigWig data into the WebApollo data directory. We'll put it in the `data/bigwig` directory. Keep in mind that this BigWig data was generated as a coverage map derived from the randomly generated BAM data, so like the BAM data there's really no biological meaning to it. We only created it to show BigWig support.

`$ `<span class="enter">`mkdir data/bigwig`</span>
`$ `<span class="enter">`cp WEB_APOLLO_SAMPLE_DIR/*.bw data/bigwig`</span>

Now we need to add the BigWig track.

`$ `<span class="enter">`bin/add-bw-track.pl --bw_url bigwig/simulated-sorted.coverage.bw \ `
`  --label simulated_bw --key "simulated BigWig"`</span>

You should now have a `simulated BigWig` track available.

### Customizing different annotation types

To change how the different annotation types look in the annotation track, you'll need to update the mapping of the annotation type to the appropriate CSS class. This data resides in `trackList.json` after running `add-webapollo-plugin.pl`. You'll need to modify the JSON entry whose label is `Annotations`. Of particular interest is the `alternateClasses` element. Let's look at that default element:

    "alternateClasses": {
        "pseudogene" : {
           "className" : "light-purple-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "tRNA" : {
           "className" : "brightgreen-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "snRNA" : {
           "className" : "brightgreen-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "snoRNA" : {
           "className" : "brightgreen-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "ncRNA" : {
           "className" : "brightgreen-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "miRNA" : {
           "className" : "brightgreen-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "rRNA" : {
           "className" : "brightgreen-80pct",
           "renderClassName" : "gray-center-30pct"
        },
        "repeat_region" : {
           "className" : "magenta-80pct"
        },
        "transposable_element" : {
           "className" : "blue-ibeam",
           "renderClassName" : "blue-ibeam-render"
        }
    },

For each annotation type, you can override the default class mapping for both `className` and `renderClassName` to use another CSS class. Check out the [Customizing features](#Customizing_features "wikilink") section for more information on customizing the CSS classes.

### Customizing features

The visual appearance of biological features in WebApollo (and JBrowse) is handled by CSS stylesheets. Every feature and subfeature is given a default CSS "class" that matches a default CSS style in a CSS stylesheet. These styles are are defined in `TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse/track_styles.css` and `TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse/plugins/WebApollo/css/webapollo_track_styles.css`. Additional styles are also defined in these files, and can be used by explicitly specifying them in the --className, --subfeatureClasses, --renderClassname, or --arrowheadClass parameters to flatfile-to-json.pl. See example [above](#GFF3_with_gene/transcript/exon/CDS/polypeptide_features "wikilink")

WebApollo differs from JBrowse in some of it's styling, largely in order to help with feature selection, edge-matching, and dragging. WebApollo by default uses invisible container elements (with style class names like "container-16px") for features that have children, so that the children are fully contained within the parent feature. This is paired with another styled element that gets rendered *within* the feature but underneath the subfeatures, and is specified by the --renderClassname argument to flatfile-to-json.pl. Exons are also by default treated as special invisible containers, which hold styled elements for UTRs and CDS.

It is relatively easy to add other stylesheets that have custom style classes that can be used as parameters to flatfile-to-json.pl. An example is `TOMCAT_WEBAPPS_DIR/WebApollo/jbrowse/sample_data/custom_track_styles.css` which contains two new styles:

    .gold-90pct, 
    .plus-gold-90pct, 
    .minus-gold-90pct  {
        background-color: gold;
        height: 90%;
        top: 5%;
        border: 1px solid gray;
    }

    .dimgold-60pct, 
    .plus-dimgold-60pct, 
    .minus-dimgold-60pct  {
        background-color: #B39700;
        height: 60%;
        top: 20%;
    }

In this example, two subfeature styles are defined, and the *top* property is being set to (100%-height)/2 to assure that the subfeatures are centered vertically within their parent feature. When defining new styles for features, it is important to specify rules that apply to plus-*stylename* and minus-*stylename* in addition to *stylename*, as WebApollo adds the "plus-" or "minus-" to the class of the feature if the the feature has a strand orientation.

You need to tell WebApollo where to find these styles. This can be done via standard CSS loading in the index.html file by adding a <link> element:

<link rel="stylesheet" type="text/css" href="sample_data/custom_track_styles.css">

Or alternatively, to avoid modifying the web application, additional CSS can be specified in the trackList.json file that is created in the data directory during static data generation, by adding a "css" property to the JSON data:

       "css" : "sample_data/custom_track_styles.css" 

Then these new styles can be used as arguments to flatfile-to-json.pl, for example:

    bin/flatfile-to-json.pl --gff WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff 
    --getSubfeatures --type mRNA --trackLabel maker --webApollo 
    --subfeatureClasses '{"CDS":"gold-90pct", "UTR": "dimgold-60pct"}' 

Depending on how your Tomcat server is setup, you might need to restart the server to pick up all the changes (or at least restart the WebApollo web application). You'll also need to do this any time you change the configuration files (not needed when changing the data files).

### Bulk loading annotations to the user annotation track

#### GFF3

You can use the `WEB_APOLLO_DIR/tools/data/add_transcripts_from_gff3_to_annotations.pl` script to bulk load GFF3 files with transcripts to the user annotation track. Let's say we want to load our `maker.gff` transcripts.

`$ `<span class="enter">`WEB_APOLLO_DIR/tools/data/add_transcripts_from_gff3_to_annotations.pl \`
`-U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \`
`-i WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff`</span>

The default options should be handle GFF3 most files that contain genes, transcripts, and exons.

You can still use this script even if the file you're loading does not contain transcripts and exons. Let's say we want to load `match` and `match_part` features as transcripts and exons respectively. We'll use the `blastn.gff` file as an example.

`$ `<span class="enter">`WEB_APOLLO_DIR/tools/data/add_transcripts_from_gff3_to_annotations.pl \`
`-U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \`
`-i WEB_APOLLO_SAMPLE_DIR/split_gff/blastn.gff -t match -e match_part`</span>

Look at the script's help (`-h`) for all available options.

Congratulations, you're done configuring WebApollo!

Upgrading existing instances
----------------------------

We suggest creating a new instance to prevent disruption to existing instances and to have a staging site before making the upgrade public. Since the local storage is file based, you can just copy the BerkeleyDB databases to another directory and have the new instance point to it:

`$ `<span class="enter">`cp -R WEB_APOLLO_DATA_DIR WEB_APOLLO_DATA_DIR_STAGING`</span>

Create a staging instance in your `TOMCAT_WEBAPPS_DIR`:

`$ `<span class="enter">`cd TOMCAT_WEBAPPS_DIR`<span>
`$ `<span class="enter">`mkdir WebApolloStaging`</span>

Unpack the WAR in the WebApoloStaging and point `&lt;datastore_directory&gt;` in `TOMCAT_WEBAPPS_DIR/WebApolloStaging/config.xml` file to wherever `WEB_APOLLO_DATA_DIR_STAGING` is. Afterwards, just setup the configuration as normal.

To use the existing static data, we can just copy the data symlink (or directory if you chose not to use a symlink):

`$ `<span class="enter">`cp -R WebApollo/jbrowse/data WebApolloStaging/jbrowse/data`

You can also copy over any custom CSS modifications you may have made to the staging site.

Once you've had a chance to test out the upgrade and make sure everything's working fine, just delete (or move it somewhere else for backup purposes) and rename the staging site:

`$ `<span class="enter">`rm -rf WebApollo`</span>
`$ `<span class="enter">`mv WebApolloStaging WebApollo`</span>

You might also want to update `&lt;datastore_directory&gt;` back to `WEB_APOLLO_DATA_DIR` and delete `WEB_APOLLO_DATA_DIR_STAGING` so that you can continue to keep the data in the same location. It's also recommended that you restart Tomcat after this.

#### Upgrading existing JBrowse data stores

You'll need to upgrade the `trackList.json` file in your JBROWSE\_DATA\_DIR directory. The WebApollo plugin needs to be reconfigured, so run through the steps in the [Adding the WebApollo plugin](#Adding_the_WebApollo_plugin "wikilink") section.

#### Upgrading existing annotation data stores

##### Transcript type updating

Releases 2013-09-04 and prior only supported annotating protein coding genes. WebApollo now supports annotating other feature types. If you're running WebApollo on annotation data generated from the 2013-09-04 and prior releases, you might want to run a tool that will update all protein coding transcripts from type "sequence:transcript" to "sequence:mRNA". Although this step is not required (WebApollo has proper backwards support for the generic "sequence:transcript" type, we recommend updating your data.

Although issues with the update are not expected, it's highly recommended to backup the databases before the update (you can delete them once you've tested the update and made sure that everything's ok).

`$ `<span class="enter">`cp -R WEB_APOLLO_DATA_DIR WEB_APOLLO_DATA_DIR.bak`</span>

Note that before you run the update, you'll need to stop WebApollo (either by shutting down Tomcat or stopping WebApollo through Tomcat's Application Manager).

You'll need to run `update_transcript_to_mrna.sh`, located in WEB\_APOLLO\_DIR/tools/data. You'll only need to run this tool when first upgrading your WebApollo version. You can either choose to run the tool on individual annotation data stores (using the `-i` option) or more conveniently run through all the data stores that are within a parent directory (using the `-d` option). We'll go ahead with the later. You can choose to update either the annotation data store or the history data store (using the `-H` option). You'll need to tell the tool where you deployed WebApollo (using the `-w` option).

`$ `<span class="enter">`WEB_APOLLO_DIR/tools/data/update_transcripts_to_mrna.sh -w TOMCAT_WEBAPPS_DIR/WebApollo -d WEB_APOLLO_DATA_DIR`</span>
`$ `<span class="enter">`WEB_APOLLO_DIR/tools/data/update_transcripts_to_mrna.sh -w TOMCAT_WEBAPPS_DIR/WebApollo -d WEB_APOLLO_DATA_DIR -H`</span>

Restart WebApollo and test out that the update didn't break anything. Once you're satisfied, you can go ahead and remove the backup we made:

`$ `<span class="enter">`rm -rf WEB_APOLLO_DATA_DIR.bak`</span>

##### Sequence alterations updating

We've modified how sequence alterations are indexed compared to releases 2013-09-04 and prior. If you're running WebApollo on annotation data generated from the 2013-09-04 and prior releases, you'll need to run a tool that will update all your sequence alterations. You only need to run this tool if you've annotated sequence alterations (e.g., insertion, deletion, substitution). If you haven't annotated those types, you can skip this step.

Although issues with the update are not expected, it's highly recommended to backup the databases before the update (you can delete them once you've tested the update and made sure that everything's ok).

`$ `<span class="enter">`cp -R WEB_APOLLO_DATA_DIR WEB_APOLLO_DATA_DIR.bak`</span>

Note that before you run the update, you'll need to stop WebApollo (either by shutting down Tomcat or stopping WebApollo through Tomcat's Application Manager).

You'll need to run `update_sequence_alterations.sh`. You can get the tarball [here](http://genomearchitect.org/webapollo/releases/patches/2013-11-22/update_sequence_alterations.tgz).

Uncompress the tarball:

`$ `<span class="enter">`tar -xvzf update_sequence_alterations.tgz`</span>
`$ `<span class="enter">`cd update_sequence_alterations`</span>

You'll only need to run this tool when first upgrading your WebApollo version. You can either choose to run the tool on individual annotation data stores (using the `-i` option) or more conveniently run through all the data stores that are within a parent directory (using the `-d` option). We'll go ahead with the later. You'll need to tell the tool where you deployed WebApollo (using the `-w` option).

`$ `<span class="enter">`./update_sequence_alterations.sh -w TOMCAT_WEBAPPS_DIR/WebApollo -d WEB_APOLLO_DATA_DIR`</span>

Restart WebApollo and test out that the update didn't break anything. Once you're satisfied, you can go ahead and remove the backup we made:

`$ `<span class="enter">`rm -rf WEB_APOLLO_DATA_DIR.bak`</span>

