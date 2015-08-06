# Data generation pipeline

The data generation pipeline is based on the typical jbrowse commands such as prepare-refseqs.pl and flatfile-to-json.pl,
 and it is installed automatically using the `apollo deploy` or `install_jbrowse.sh` commands.

If you have setup webapollo properly using these steps, then a bin/ subdirectory will be initialized with the jbrowse
perl scripts. If this does not exist, please check setup.log to see where the error might be, and check the [troubleshooting
guide](Troubleshooting.md).

### prepare-refseqs.pl

The first step to setup the genome browser is to load the reference genome data. We'll use the `prepare-refseqs.pl`
script to output to the data directory that we will point to later in the organism tab.

```
    bin/prepare-refseqs.pl --fasta pyu_data/scf1117875582023.fa --out /opt/apollo/data
````


### flatfile-to-json.pl

The flatfile-to-json.pl script can be used to setup a GFF3 tracks with flexible feature types. Here, we'll start off by loading data from the MAKER generated GFF for the Pythium ultimum data. The simplest loading command specifies a --trackLabel, the --type of feature to load, the --gff file and the --out directory.

```
    bin/flatfile-to-json.pl --gff pyu_data/scf1117875582023.gff --type mRNA --trackLabel MAKER --out /opt/apollo/data
````
 
Note: you can also use simple scripts like bin/maker2jbrowse to expedite loading of MAKER data.

Also see the section [Customizing features](Data_loading.md#customizing-features) section for more information on customizing the CSS styles of the Web Apollo 2.0 features.


### generate-names.pl

Once data tracks have been created, you can generate a searchable index of names using the generate-names.pl script:

```
    bin/generate-names.pl --verbose --out /opt/apollo/data
```

This script creates an index of sequence names and feature names in order to enable auto-completion in the navigation
text box. If you have some tracks that have millions of features, consider using "--completionLimit 0" to disable the
autocompletion which will save time, and you can also incrementally index the features with the --incremental
option to disable the completion on certain tracks.

### add-bam-track.pl

WebApollo natively supports BAM files and the file can be read (in chunks) directly from the server with no preprocessing.

To add a BAM track, copy the BAM+BAI files to your data directory, and then use the add-bam-track.pl to add the file to the tracklist.

```
    mkdir /opt/apollo/data/bam
    cp pyu_data/simulated-sorted.bam* /opt/apollo/data/bam
    bin/add-bam-track.pl --bam_url bam/simulated-sorted.bam \
       --label simulated_bam --key "simulated BAM" -i /opt/apollo/data/trackList.json
```


Note: the `bam_url` parameter is a URL that is relative to the data directory. It is not a filepath!

### add-bw-track.pl

WebApollo also has native support for BigWig files (.bw), so no extra processing of these files is required either.

To use this, copy the BigWig data into the jbrowse data directory and then use the add-bw-track.pl to add the file to
the tracklist.

```
    mkdir /opt/apollo/data/bigwig
    cp pyu_data/*.bw /opt/apollo/data/bigwig
    bin/add-bw-track.pl --bw_url bigwig/simulated-sorted.coverage.bw \
        --label simulated_bw --key "simulated BigWig"
```

Note: the `bw_url` parameter is a URL that is relative to the data directory. It is not a filepath!

### Customizing different annotation types (advanced)

To change how the different annotation types look in the "User-created annotation" track, you'll need to update the mapping of the
annotation type to the appropriate CSS class. This data resides in `client/apollo/json/annot.json`, which is a file containing WebApollo tracks that is loaded by default. You'll need to modify the JSON entry whose label is `Annotations`. Of particular interest is
the `alternateClasses` element. Let's look at that default element:

```
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
    }
```

For each annotation type, you can override the default class mapping for both `className` and `renderClassName` to use
another CSS class. Check out the [Customizing features](Data_loading.md#customizing-features) section for more
information on customizing the CSS classes.

### Customizing features

The visual appearance of biological features in WebApollo (and JBrowse) is handled by CSS stylesheets with HTMLFeatures
tracks. Every feature and subfeature is given a default CSS "class" that matches a default CSS style in a CSS
stylesheet. These styles are are defined in `client/apollo/css/track_styles.css` and
`client/apollo/css/webapollo_track_styles.css`. Additional styles are also defined in these
files, and can be used by explicitly specifying them in the --className, --subfeatureClasses, --renderClassname, or
--arrowheadClass parameters to flatfile-to-json.pl ([see data loading section](Data_loading.md#flatfile-to-json.pl_transcripts)).

WebApollo differs from JBrowse in some of it's styling, largely in order to help with feature selection, edge-matching,
and dragging. WebApollo by default uses invisible container elements (with style class names like "container-16px") for
features that have children, so that the children are fully contained within the parent feature. This is paired with
another styled element that gets rendered *within* the feature but underneath the subfeatures, and is specified by the
`--renderClassname` argument to `flatfile-to-json.pl`. Exons are also by default treated as special invisible
containers, which hold styled elements for UTRs and CDS.

It is relatively easy to add other stylesheets that have custom style classes that can be used as parameters to
`flatfile-to-json.pl`. For example, you can create `/opt/apollo/data/custom_track_styles.css` which contains two new
styles:

```
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
```


In this example, two subfeature styles are defined, and the *top* property is being set to (100%-height)/2 to assure
that the subfeatures are centered vertically within their parent feature. When defining new styles for features, it is
important to specify rules that apply to plus-*stylename* and minus-*stylename* in addition to *stylename*, as WebApollo
adds the "plus-" or "minus-" to the class of the feature if the the feature has a strand orientation.

You need to tell WebApollo where to find these styles by modifying the JBrowse config or the plugin config, e.g. by
adding this to the trackList.json

```
       "css" : "data/custom_track_styles.css" 
```

Then you may use these new styles using --subfeatureClasses, which uses the specified CSS classes for your features in the genome browser, for example:

```
    bin/flatfile-to-json.pl --gff MyFile.gff --type mRNA --trackLabel MyTrack
        --subfeatureClasses '{"CDS":"gold-90pct", "UTR": "dimgold-60pct"}' 
```

### Bulk loading annotations to the user annotation track

#### GFF3

You can use the `tools/data/add_transcripts_from_gff3_to_annotations.pl` script to bulk load GFF3 files with transcripts
to the user annotation track. Let's say we want to load our `maker.gff` transcripts.

```
    tools/data/add_transcripts_from_gff3_to_annotations.pl \
        -U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \
        -i scf1117875582023.gff -type mRNA
```


The default options should be handle GFF3 most files that contain genes, transcripts, and exons.

You can still use this script even if the GFF3 file that you are loading does not contain transcripts and exon types.
Let's say we want to load `match` and `match_part` features as transcripts and exons respectively. We'll use the
`blastn.gff` file as an example.

```
    tools/data/add_transcripts_from_gff3_to_annotations.pl \
       -U localhost:8080/WebApollo -u web_apollo_admin -p web_apollo_admin \
       -i cf1117875582023gff -t match -e match_part
```



You can view the add_transcripts_from_gff3_to_annotations.pl help (`-h`) option for all available options.


### Special track config parameters


Web Apollo has a number of specific track config parameters


    overrideDraggable (boolean)
    determines whether to transform the alignments tracks to draggable alignments. avaiable since 2.x

    overridePlugins (boolean)
    determines whether to transform alignments and sequence tracks. available since early and 1.x versions of WA

These can be specified on a specific track or globally (i.e. outside the "tracks" section of trackList.json)
