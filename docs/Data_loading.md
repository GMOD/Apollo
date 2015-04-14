# Data generation pipeline

View <a href="https://github.com/GMOD/Apollo/blob/master/docs/Data_loading.md">On GitHub</a>

The data generation pipeline, based on the typical jbrowse commands such as prepare-refseqs.pl and flatfile-to-json.pl,
is installed automatically from `apollo deploy` or `install_jbrowse.sh`

If you have setup webapollo properly using these steps, then a bin/ subdirectory will be initializedwith the jbrowse
perl scripts. If this does not exist, please check setup.log to see where the error might be, check the [troubleshooting
guide](Troubleshooting.md), and post to apollo@lists.lbl.gov for further assistance.

### prepare-refseqs.pl

The first step to setup the genome browser is to load the reference genome data. We'll use the `prepare-refseqs.pl`
script to output to the data directory that we configured in config.properties or config.xml.

    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out /opt/apollo/data

### add-webapollo-plugin.pl

After initializing the data directory, add the WebApollo plugin tracks using the `add-webapollo-plugin.pl`. It takes a
'trackList.json' as an argument.

    bin/add-webapollo-plugin.pl -i /opt/apollo/data/trackList.json

### split_gff_by_source.pl

Generating data from GFF3 works best by having a separate GFF3 per source type. If your GFF3 has all source types in the
same file, as with the Pythium ultimum sample, then use the  `tools/data/split_gff_by_source.pl` script. We'll output
the split GFF3 to some temporary directory (e.g. `split_gff`).

    mkdir split_gff
    tools/data/split_gff_by_source.pl -i pyu_data/scf1117875582023.gff -d split_gff

If we look at the contents of `split_gff`, we can see we have the following files:

    blastn.gff  est2genome.gff  protein2genome.gff  repeatrunner.gff
    blastx.gff  maker.gff       repeatmasker.gff    snap_masked.gff

We will load each file and create the appropriate tracks in the following steps.

#### flatfile-to-json.pl transcripts

The flatfile-to-json script can setup a GFF3 track with gene/transcript/exon/CDS/polypeptide features. We'll start off
by loading `maker.gff` from the Pythium ultimum data. The simplest loading command specifies a trackLabel, the type of
feature to load [1] and an input file and an output directory.

    bin/flatfile-to-json.pl --gff split_gff/maker.gff --type mRNA --trackLabel maker --out /opt/apollo/data

See the [Customizing features](Data_loading.md#customizing-features) section for more information on customizing the CSS
styles of the Web Apollo 2.0 features.

#### flatfile-to-json.pl match features

If your track uses match and match_part types instead of gene->mRNA->exon, you can load the track using the --type match
argument.

    bin/flatfile-to-json.pl --gff split_gff/blastn.gff \
      --arrowheadClass webapollo-arrowhead \
      --subfeatureClasses '{"match_part": "darkblue-80pct"}'
      --type match
      --className container-10px --trackLabel blastn --out /opt/apollo/data


#### generate-names.pl

Once data tracks have been created, you can generate a searchable index of names using the generate-names.pl script:

    bin/generate-names.pl --verbose --out /opt/apollo/data

This script creates an index of sequence names and feature names in order to enable auto-completion in the navigation
text box. If you have some tracks that have millions of features, consider using "--completionLimit 0" to disable the
autocompletion which will save time.

#### add-bam-track.pl

BAM files are natively supported so the file can be read (in chunks) directly from the server with no preprocessing.

To use this, copy the BAM+BAM index to your jbrowse data directory, and then use the add-bam-track.pl to add the file to
the tracklist.

    mkdir /opt/apollo/data/bam
    cp pyu_data/simulated-sorted.bam /opt/apollo/data/bam
    cp pyu_data/simulated-sorted.bam.bai /opt/apollo/data/bam
    bin/add-bam-track.pl --bam_url bam/simulated-sorted.bam \
       --label simulated_bam --key "simulated BAM" -i /opt/apollo/data/trackList.json

Note: the `bam_url` parameter is a relative URL to the data directory. It is not a filepath!

#### add-bw-track.pl

WebApollo also has native support for BigWig files (.bw), so no extra processing of these files is required either.

To use this, copy the BigWig data into the jbrowse data directory and then use the add-bw-track.pl to add the file to
the tracklist.

    mkdir /opt/apollo/data/bigwig
    cp pyu_data/*.bw /opt/apollo/data/bigwig

Now we need to add the BigWig track.

    bin/add-bw-track.pl --bw_url bigwig/simulated-sorted.coverage.bw \ `
      --label simulated_bw --key "simulated BigWig"`</span>

Note: the `bw_url` paramter is a relative URL to the data directory. It is not a filepath!

### Customizing different annotation types (advanced)

After running `add-webapollo-plugin.pl`, the annotation track will be added to `trackList.json`. To change how the
different annotation types look in the "User-created annotation" track, you'll need to update the mapping of the
annotation type to the appropriate CSS class. This data resides in `trackList.json` after running
`add-webapollo-plugin.pl`. You'll need to modify the JSON entry whose label is `Annotations`. Of particular interest is
the `alternateClasses` element. Let's look at that default element:

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

For each annotation type, you can override the default class mapping for both `className` and `renderClassName` to use
another CSS class. Check out the [Customizing features](Data_loading.md#customizing-features) section for more
information on customizing the CSS classes.

### Customizing features

The visual appearance of biological features in WebApollo (and JBrowse) is handled by CSS stylesheets with HTMLFeatures
tracks. Every feature and subfeature is given a default CSS "class" that matches a default CSS style in a CSS
stylesheet. These styles are are defined in `src/main/webapps/jbrowse/plugins/WebApollo/jbrowse/track_styles.css` and
`src/main/webapps/jbrowse/plugins/WebApollo/css/webapollo_track_styles.css`. Additional styles are also defined in these
files, and can be used by explicitly specifying them in the --className, --subfeatureClasses, --renderClassname, or
--arrowheadClass parameters to flatfile-to-json.pl [see section](Data_loading.md#flatfile-to-json.pl_transcripts).

WebApollo differs from JBrowse in some of it's styling, largely in order to help with feature selection, edge-matching,
and dragging. WebApollo by default uses invisible container elements (with style class names like "container-16px") for
features that have children, so that the children are fully contained within the parent feature. This is paired with
another styled element that gets rendered *within* the feature but underneath the subfeatures, and is specified by the
`--renderClassname` argument to `flatfile-to-json.pl`. Exons are also by default treated as special invisible
containers, which hold styled elements for UTRs and CDS.

It is relatively easy to add other stylesheets that have custom style classes that can be used as parameters to
`flatfile-to-json.pl`. For example, you can create `/opt/apollo/data/custom_track_styles.css` which contains two new
styles:

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

In this example, two subfeature styles are defined, and the *top* property is being set to (100%-height)/2 to assure
that the subfeatures are centered vertically within their parent feature. When defining new styles for features, it is
important to specify rules that apply to plus-*stylename* and minus-*stylename* in addition to *stylename*, as WebApollo
adds the "plus-" or "minus-" to the class of the feature if the the feature has a strand orientation.

You need to tell WebApollo where to find these styles by modifying the JBrowse config or the plugin config, e.g. by
adding this to the trackList.json

       "css" : "data/custom_track_styles.css" 

Then you may use these new styles when loading tracks to flatfile-to-json.pl, for example:

    bin/flatfile-to-json.pl --gff WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff 
        --getSubfeatures --type mRNA --trackLabel maker --webApollo 
        --subfeatureClasses '{"CDS":"gold-90pct", "UTR": "dimgold-60pct"}' 


### Bulk loading annotations to the user annotation track

#### GFF3

You can use the `tools/data/add_transcripts_from_gff3_to_annotations.pl` script to bulk load GFF3 files with transcripts
to the user annotation track. Let's say we want to load our `maker.gff` transcripts.

    tools/data/add_transcripts_from_gff3_to_annotations.pl \
        -U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \
        -i WEB_APOLLO_SAMPLE_DIR/split_gff/maker.gff

The default options should be handle GFF3 most files that contain genes, transcripts, and exons.

You can still use this script even if the GFF3 file that you are loading does not contain transcripts and exon types.
Let's say we want to load `match` and `match_part` features as transcripts and exons respectively. We'll use the
`blastn.gff` file as an example.

    tools/data/add_transcripts_from_gff3_to_annotations.pl \
       -U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \
       -i split_gff/blastn.gff -t match -e match_part

Look at the script's help (`-h`) for all available options.

Congratulations, you're done configuring WebApollo!
