## WebApollo Configuration

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Configure.md">On GitHub</a>

WebApollo 2.0 has a basic configuration that is mostly configured using the web interface or through simple changes to Config.groovy

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

You can configure a set of predefined comments that will be available for users when adding comments through a dropdown
menu based on `canned_comments.xml`. Let’s take a look at the configuration file.

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

All [supported annotation types](Configure.md#supported-annotation-types) can be used.

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

The GFF3 data adapter will allow exporting the current annotations as a GFF3 file. You can get more information about the GFF3 format at [The Sequence Ontology GFF3 page](http://www.sequenceontology.org/gff3.shtml). The configuration is stored in `gff3_config.xml`. Let’s take a look at the configuration file:

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

This defines which metadata to export in the GFF3 (in column 9). This configuration is optional. The default is to export everything except owner, date\_creation, and date\_last\_modified. You need to define one `<metadata>` element with the appropriate `type` attribute per metadata type you want to export. Available types are:

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

Note that the generated files will reside in that directory indefinitely to allow users to download them. You'll need to eventually remove those files to prevent the file system from cluttering up. There's a script that will traverse the directory and remove any files that are older than a provided time and cleanup directories as they become empty. It's recommended to setup this script as a `cron` job that runs hourly to remove any files older than an hour (should provide plenty of time for users to download those files). The script is in `tools/cleanup/remove_temporary_files.sh`.

`$ tools/cleanup/remove_temporary_files.sh -d TOMCAT_WEBAPPS_DIR/WebApollo/tmp -m 60`


#### FASTA

The FASTA data adapter will allow exporting the current annotations to a FASTA file. The configuration is stored in `src/main/webapp/config/fasta_config.xml`. Let’s take a look at the configuration file:

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

This defines which annotation types should be processed when exporting the FASTA data. You'll need one `<feature_type>` element for each type you want to have processed. Only the defined `feature_type` elements will all be processed, so you might want to have different configuration files for processing different types of annotations (which you can point to in FASTA data adapter in the `config` element in `config.xml`). All [supported annotation types](Configure.md#supported-annotation-types) can be used for the value of `feature_type`, with the addition of `sequence:exon`.

In `config.xml`, in the `<options>` element in the `<data_adapter>` configuration for the FASTA adapter, you'll notice that there's a `seqType` option. You can change that value to modify which type of sequence will be exported as FASTA. Available options are:

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

Defines which metadata to export in the defline for each feature. The default is to not output any of the listed metadata. Uncomment to turn on this option. Note that you can remove (or comment) any `<metadata>` elements that you're not interested in exporting.

Note that like the GFF3 adapter, the generated files will reside in that directory indefinitely to allow users to download them. You'll need to eventually remove those files to prevent the file system from cluttering up. You can use the `remove_temporary_files.sh` script to handle the cleanup. In fact, if you configure both the GFF3 and FASTA adapters to use the same temporary directory, you'll only need to worry about cleanup from a single location. See the [GFF3](Configure.md#gff3) section for information about `remove_temporary_files.sh`.



### Upgrading existing instances

Todo


#### Upgrading existing JBrowse data stores

The JBrowse data stores have remained stable so no changes to existing JBrowse data tracks are necessary in Web Apollo 2.0.

##### Sequence alterations updating

Todo
