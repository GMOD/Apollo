# Data generation pipeline


The data generation pipeline will use jbrowse and webapollo perl scripts. The preferred way to install these is through the `apollo deploy` command, which will install the necessary pre-requisites automatically into a local directory.

If you have trouble finding or running the jbrowse binaries, please refer to the [troubleshooting guide](Troubleshooting.md). If you are building a custom jbrowse+webapollo package, please refer to the [developers guide](Developer.md). This directory will output the data into a such as JBROWSE_DATA_DIR like in the [Quick-install guide](Quick_start_guide.md), and will also be using the pyu_data that was sourced there as well.

Note: you will be based in the directory that you originally downloaded or cloned Web Apollo during these steps, not inside the tomcat webapps directory.

### DNA track setup

The first step to setup the genome browser is to load the reference genome data to be used by JBrowse. We'll use the `prepare-refseqs.pl` script.

    $ bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out $JBROWSE_DATA_DIR

We now have the DNA track setup. Note that you can also use a GFF3 file if it contains the genomic sequence itself by using the `--gff` option instead of `--fasta` and point it to the GFF3 file.

### Adding the WebApollo plugin

We now need to setup the WebApollo plugin tracks using the `add-webapollo-plugin.pl`. It takes a 'trackList.json' as an argument.

    $ client/apollo/bin/add-webapollo-plugin.pl -i $JBROWSE_DATA_DIR/trackList.json

### Process Pythium ultimum GFF3

Generating data from GFF3 works best by having a separate GFF3 per source type. If your GFF3 has all source types in the same file, as with the Pythium ultimum sample, then use the  `tools/data/split_gff_by_source.pl` script. We'll output the split GFF3 to some temporary directory (e.g. `split_gff`).

    $ mkdir split_gff
    $ tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d split_gff

If we look at the contents of `WEB_APOLLO_SAMPLE_DIR/split_gff`, we can see we have the following files:

    $ ls split_gff
    blastn.gff  est2genome.gff  protein2genome.gff  repeatrunner.gff
    blastx.gff  maker.gff       repeatmasker.gff    snap_masked.gff

We will load each file and create the appropriate tracks in the following steps.

#### Load GFF3 with gene/transcript/exon/CDS/polypeptide features

We'll start off by loading `maker.gff` from the Pythium ultimum data. We need to handle that file a bit differently than the rest of the files since the GFF represents the features as gene, transcript, exons, and CDSs.

    $ bin/flatfile-to-json.pl --gff split_gff/maker.gff \
      --arrowheadClass trellis-arrowhead  \
      --subfeatureClasses '{"wholeCDS": null, "CDS":"brightgreen-80pct", "UTR": "darkgreen-60pct", "exon":"container-100pct"}' \
      --className container-16px --type mRNA --trackLabel maker --out $JBROWSE_DATA_DIR

Note: it is important to use `--type mRNA` to maintain database consistency. If there are three levels of gene features, such as gene->mRNA->exon, then you want to load the "mRNA" level.


Also note: `brightgreen-80pct`, `darkgreen-60pct`, `container-100pct`, `container-16px`, `gray-center-20pct` are all CSS classes defined in WebApollo stylesheets that describe how to display their respective features and subfeatures. WebApollo also tries to use reasonable default CSS styles, so it is possible to omit these CSS class arguments. For example, to accept default styles for maker.gff, the above could instead be shortened to:

    $ bin/flatfile-to-json.pl --gff split_gff/maker.gff \
      --getSubfeatures --type mRNA --trackLabel maker --out $JBROWSE_DATA_DIR

See the [Customizing features](Data_loading.md#Customizing_features) section for more information on CSS styles. There are also many other configuration options for flatfile-to-json.pl, see [JBrowse data formatting](JBrowse_Configuration_Guide#Data_Formatting) for more information.

#### GFF3 with match/match\_part features

Now we need to process the other remaining GFF3 files. The entries in those are stored as "match/match\_part", so they can all be handled in a similar fashion.

We'll start off with `blastn` results as an example.

    $bin/flatfile-to-json.pl --gff split_gff/blastn.gff \
      --arrowheadClass webapollo-arrowhead --getSubfeatures \
      --subfeatureClasses '{"match_part": "darkblue-80pct"}' \
      --className container-10px --trackLabel blastn --out $JBROWSE_DATA_DIR

You can see the the --type parameter is not needed here because there are only two levels to the blastn.gff, match->match_part.

You can automate this process to load all the rest of the tracks similarly as follows:

    for i in $(ls split_gff/*.gff | grep -v maker); do
        j=$(basename $i)
        j=${j/.gff/}
        echo "Processing $j"
        bin/flatfile-to-json.pl --gff $i --arrowheadClass webapollo-arrowhead \
            --getSubfeatures --subfeatureClasses "{\"match_part\": \"darkblue-80pct\"}" \
            --className container-10px --trackLabel $j --out $JBROWSE_DATA_DIR
    done


#### Generate searchable name index

Once data tracks have been created, you can generate a searchable index of names using the generate-names.pl script:

    $ bin/generate-names.pl --out $JBROWSE_DATA_DIR

This script creates an index of sequence names and feature names in order to enable auto-completion in the navigation text box.

The script can be also incrementally get new data by using the `--incremental` option.


#### BAM data

WebApollo/JBrowse has native support for BAM files, so the file can be read (in chunks) directly from the server with no preprocessing.

First we'll copy the BAM and BAM index file into $JBROWSE_DATA_DIR.

    $ mkdir $JBROWSE_DATA_DIR/bam
    $ cp pyu_data/*.bam* $JBROWSE_DATA_DIR/bam

Then we need to add the BAM track to the track configuration.

    $ bin/add-bam-track.pl --bam_url bam/simulated-sorted.bam \
       --label simulated_bam --key "simulated BAM" -i $JBROWSE_DATA_DIR/trackList.json

Note: the `bam_url` parameter is a relative URL to the $JBROWSE_DATA_DIR. It is not a filepath!

#### BigWig data

WebApollo has native support for BigWig files (.bw), so no extra processing of the data is required.

Configuring a BigWig track is very similar to configuring a BAM track. First we'll copy the BigWig data into the WebApollo data directory. We'll put it in the `$JBROWSE_DATA_DIR/bigwig` directory.

    $ mkdir $JBROWSE_DATA_DIR/bigwig
    $ cp pyu_data/*.bw $JBROWSE_DATA_DIR/bigwig

Now we need to add the BigWig track.

    $bin/add-bw-track.pl --bw_url bigwig/simulated-sorted.coverage.bw \ `
      --label simulated_bw --key "simulated BigWig"`</span>

Note: the `bw_url` paramter is a relative URL to the $JBROWSE_DATA_DIR. It is not a filepath!

### Customizing different annotation types (advanced)

After running `add-webapollo-plugin.pl`, the annotation track will be added to `trackList.json`. To change how the different annotation types look in the "User-created annotation" track, you'll need to update the mapping of the annotation type to the appropriate CSS class. This data resides in `trackList.json` after running `add-webapollo-plugin.pl`. You'll need to modify the JSON entry whose label is `Annotations`. Of particular interest is the `alternateClasses` element. Let's look at that default element:

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

For each annotation type, you can override the default class mapping for both `className` and `renderClassName` to use another CSS class. Check out the [Customizing features](Data_loading.md#Customizing_features) section for more information on customizing the CSS classes.

### Customizing features

The visual appearance of biological features in WebApollo (and JBrowse) is handled by CSS stylesheets with HTMLFeatures tracks. Every feature and subfeature is given a default CSS "class" that matches a default CSS style in a CSS stylesheet. These styles are are defined in `src/main/webapps/jbrowse/plugins/WebApollo/jbrowse/track_styles.css` and `src/main/webapps/jbrowse/plugins/WebApollo/css/webapollo_track_styles.css`. Additional styles are also defined in these files, and can be used by explicitly specifying them in the --className, --subfeatureClasses, --renderClassname, or --arrowheadClass parameters to flatfile-to-json.pl. See example [above](#Load_GFF3_with_gene/transcript/exon/CDS/polypeptide_features)

WebApollo differs from JBrowse in some of it's styling, largely in order to help with feature selection, edge-matching, and dragging. WebApollo by default uses invisible container elements (with style class names like "container-16px") for features that have children, so that the children are fully contained within the parent feature. This is paired with another styled element that gets rendered *within* the feature but underneath the subfeatures, and is specified by the --renderClassname argument to flatfile-to-json.pl. Exons are also by default treated as special invisible containers, which hold styled elements for UTRs and CDS.

It is relatively easy to add other stylesheets that have custom style classes that can be used as parameters to flatfile-to-json.pl. For example, you can create `$JBROWSE_DATA_DIR/custom_track_styles.css` which contains two new styles:

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

You need to tell WebApollo where to find these styles by modifying the JBrowse config or the plugin config, e.g. by adding this to the trackList.json

       "css" : "sample_data/custom_track_styles.css" 

Or you can also instead add the custom_track_styles.css to `src/main/webapp/plugins/WebApollo/css/` and then use the @import command in `src/main/webapp/jbrowse/plugins/WebApollo/css/main.css`. Then you may use these new styles when loading tracks to flatfile-to-json.pl, for example:

    bin/flatfile-to-json.pl --gff WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff 
        --getSubfeatures --type mRNA --trackLabel maker --webApollo 
        --subfeatureClasses '{"CDS":"gold-90pct", "UTR": "dimgold-60pct"}' 


### Bulk loading annotations to the user annotation track

#### GFF3

You can use the `tools/data/add_transcripts_from_gff3_to_annotations.pl` script to bulk load GFF3 files with transcripts to the user annotation track. Let's say we want to load our `maker.gff` transcripts.

    $ tools/data/add_transcripts_from_gff3_to_annotations.pl \
        -U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \
        -i WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff

The default options should be handle GFF3 most files that contain genes, transcripts, and exons.

You can still use this script even if the GFF3 file that you are loading does not contain transcripts and exon types. Let's say we want to load `match` and `match_part` features as transcripts and exons respectively. We'll use the `blastn.gff` file as an example.

    $ tools/data/add_transcripts_from_gff3_to_annotations.pl \
       -U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \
       -i split_gff/blastn.gff -t match -e match_part

Look at the script's help (`-h`) for all available options.

Congratulations, you're done configuring WebApollo!
