# Web Service API


The Apollo Web Service API is a JSON-based REST API to interact with the annotations and other services of Web Apollo.
Both the request and response JSON objects can contain feature information that are based on the Chado schema.  We use the web services API for several 
<a href="https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/">scripting examples</a> and also use them in the Web Apollo JBrowse plugin,
and this document provides details on the parameters for each API.


## What is the Web Service API?

For a given Web Apollo server url (e.g., `https://localhost:8080/apollo` or any other Web Apollo site on the web), the Web Service API allows us to make requests to the various "controllers" of the application and perform operations.

The controllers that are available for Web Apollo include the AnnotationEditorController, the OrganismController, the IOServiceController for downloads of data, and the UserController for user management.


Most API requests will take:

* The proper url (e.g., to get features from the AnnotationEditorController, we can send requests to `http://localhost/apollo/annotationEditor/getFeatures`)
* username - an authorized user 
* password - a password
* organism - (optional) the "common name" of the organism for feature related operations -- will also pull from the "user preferences" if none is specified.
* track/sequence - (optional) reference sequence name (shown in sequence panel / genomic browse)
* uniquename - (if applicable) the uniquename is a [UUID](https://docs.oracle.com/javase/7/docs/api/java/util/UUID.html) used to guarantee a unique ID


### Errors
If an error has occurred, a proper HTTP error code (most likely 400 or 500) and an error message.
is returned, in JSON format:

``` 
{
    "error": "mergeExons(): Exons must be in the same strand"
}
```

### Additional Notes

If you are sending password you care about over the wire (even if not using web services) it is <strong>highly recommended</strong>  that you use https (which adds encryption ssl) instead of http.


### Example


``` 
curl -b cookies.txt -c cookies.txt -e "http://localhost:8080" -H "Content-Type:application/json" \
    -d "{'username': 'demo', 'password': 'demo'}" "http://localhost:8080/apollo/Login?operation=login"
```


Login expects two parameters: <code>username</code> and <code>password</code>.

Login will return a JSON containing the <code>session-id</code> for the user. This is needed if the user's
browser does not support cookies (or is turned off), in which case the <code>session-id</code> should be
appended to all subsequent requests as <code>jsessionid=session-id</code> as a URL parameter.

``` 
{"session-id":"43FBA5B967595D260A1C0E6B7052C7A1"}
```

### Representing features in JSON

Most requests and responses will contain an array of `feature` JSON objects named `features`.
The `feature` object is based on the Chado `feature`, `featureloc`, `cv`, and `cvterm` tables.

```     
{
    "residues": "$residues",
    "type": {
        "cv": {"name": "$cv_name"},
        "name": "$cv_term"
    },
    "location": {
        "fmax": $rightmost_intrabase_coordinate_of_feature,
        "fmin": $leftmost_intrabase_coordinate_of_feature,
        "strand": $strand
    },
    "uniquename": "$feature_unique_name"
    "children": [ $array_of_child_features ]
    "properties": [ $array_of_properties ]
}
```
where:

* `residues` - A sequence of alphabetic characters representing biological residues (nucleic acids, amino acids) [string]
* `type.cv.name` - The name of the ontology [string]
* `type.name` - The name for the cvterm [string]</li>
* `location.fmax` - The rightmost/maximal intrabase boundary in the linear range [integer]</li>
* `location.fmin` - The leftmost/minimal intrabase boundary in the linear range [integer]</li>
* `strand` - The orientation/directionality of the location. Should be 0, -1 or +1 [integer]</li>
* `uniquename` - The unique name for a feature [string]</li>
* `children` - Array of child feature objects [array]</li>
* `properties` - Array of properties (including frameshifts for transcripts) [array]</li>

Note that different operations will require different fields to be set (which will be elaborated upon in each operation section).


## Web Services API


### OrganismController

#### add_organism

Adds an organism to the database. An example using this script [add_organism.groovy](https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/add_organism.groovy).

Request: `/organism/addOrganism`

``` 
{
    "directory": "/opt/apollo/myanimal/jbrowse/data",
    "username": "bob@admin.gov",
    "password": "password",
    "blatdb": "/opt/apollo/myanimal/myanimal.2bit",
    "genus": "Genus",
    "species": "species"
}
```

Response Status 200:

`{}`


#### get_sequences_for_organism



Gets all sequence names for a given organism, that has annotations. An example using this operation is [migrate_annotations.groovy](https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/migrate_annotations1to2.groovy).

Request: `/organism/getSequencesForOrganism`

``` 
{
    "username": "bob@admin.gov",
    "password": "password",
    "organism": "organism_common_name"
}
```

Response Status 200:

``` 
{
    "username": "bob@admin.gov",
    "sequences": [ "chr1", "chr4", "chr11" ],
    "organism": "organism_common_name"
}
```


### AnnotationEditorController

#### add_feature

Add a top level feature. Returns feature just added.

Request: `/annotationEditor/addFeature`

``` 
{
    "features": [{
        "location": { "fmax": 2735, "fmin": 0, "strand": 1 },
        "type": { "cv": {"name": "SO"}, "name": "gene" },
        "uniquename": "gene"
    }]
}
```

Response:

``` 
{
    "features": [{
        "location": { "fmax": 2735, "fmin": 0, "strand": 1 },
        "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene"
    }]
}
```


#### delete_feature



Delete feature(s) from the session. Each feature only requires `uniquename` to be set. Returns an empty `features` array.

Request: `/annotationEditor/deleteFeature`

``` 
{ "features": [{"uniquename": "gene"}] }
```

Response:

``` 
{"features": []}
```


#### get_features


Get all top level features.

Request: `/annotationEditor/getFeatures`

```{ }```

Response:

``` 
{
    "features": [{
        "location": { "fmax": 2735, "fmin": 0, "strand": 1 },
        "type": { "cv": {"name": "SO"}, "name": "gene" },
        "uniquename": "gene"
    }]
}
```


#### add_transcript


Add transcript(s) to a gene. The first element of the `features` array should be the gene to add the transcript(s) to, with each subsequent feature being a transcript to be added. The gene feature only requires the `uniquename` field to be set. Returns the gene which the transcript was added to.

Request: `/annotationEditor/addTranscript`

``` 
{
    "features": [{
        "uniquename": "gene"
    },
    {
        "location": { "fmax": 2628, "fmin": 638, "strand": 1 },
        "type": { "cv": {"name": "SO"}, "name": "transcript" },
        "uniquename": "transcript"
    }]
}
```

Response:

```
{
    "features": [{
        "children": [{ "location": { "fmax": 2628, "fmin": 638, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" }], "location": { "fmax": 2735, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```



#### duplicate_transcript


Duplicate a transcript. Only the first transcript in the `features` array is processed. The transcript feature only requires `uniquename` to be set. Returns the parent gene of the transcript.

Request: `/annotationEditor/duplicateTranscript`

``` 
{ "features": [{"uniquename": "transcript"}] }
```

Response:

``` 
{"features": [{ "children": [ { "location": { "fmax": 2628, "fmin": 638, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript-copy" }, { "location": { "fmax": 2628, "fmin": 638, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" } ], "location": { "fmax": 2735, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```


#### merge_transcripts


Merge two transcripts together. Only the transcripts in the first and second positions in the `features` array are processed. The transcript features only requires `uniquename` to be set. If the two transcripts belong to different genes, the parent genes are merged as well. Returns the parent gene of the merged transcripts.

Request: `/annotationEditor/mergeTranscripts`

``` 
{ "features": [ {"uniquename": "transcript1"}, {"uniquename": "transcript2"} ] }
```

Response:

``` 
{"features": [{ "children": [ { "children": [ { "location": { "fmax": 2400, "fmin": 2000, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2_1" }, { "location": { "fmax": 700, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1_1" }, { "location": { "fmax": 1500, "fmin": 1000, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1_2" }, { "location": { "fmax": 2700, "fmin": 2500, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2_2" } ], "location": { "fmax": 2700, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript1" } ], "location": { "fmax": 2700, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene1" }]}
```



#### set_translation_start


Set the CDS start and end in a transcript. The transcript feature only needs to have the `uniquename` field set. The JSON transcript must contain a CDS feature, which will contain the new CDS boundaries. Other children of the transcript will be ignored. Returns the parent gene of the transcript.

Request: `/annotationEditor/setTranslationStart`

``` 
{ "features": [{ "children": [{ "location": { "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "CDS" }, "uniquename": "cds" }], "uniquename": "transcript1" }] }
```

Response:

``` 
{"features": [{ "children": [{ "children": [ { "location": { "fmax": 700, "fmin": 500, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2_1" }, { "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1_1" }, { "location": { "fmin": 100, "fmin": 1400, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "CDS" }, "uniquename": "transcript1-CDS" }, { "location": { "fmax": 1400, "fmin": 1200, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2_3" }, { "location": { "fmax": 1000, "fmin": 800, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1_2" } ], "location": { "fmax": 1400, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript1" }], "location": { "fmax": 1500, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```



#### set_translation_end


Set the CDS end in a transcript. The transcript feature only needs to have the `uniquename` field set. The JSON transcript must contain a CDS feature, which will contain the new CDS boundaries. Other children of the transcript will be ignored. Returns the parent gene of the transcript.

Request: `/annotationEditor/setTranslationEnd`

``` 
{ "features": [{ "children": [{ "location": { "fmax": 1300, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "CDS" }, "uniquename": "cds" }], "uniquename": "transcript1" }] }
```

Response:

``` 
{"features": [{ "children": [{ "children": [ { "location": { "fmax": 700, "fmin": 500, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2_1" }, { "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1_1" }, { "location": { "fmax": 1300, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "CDS" }, "uniquename": "transcript1-CDS" }, { "location": { "fmax": 1400, "fmin": 1200, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2_3" }, { "location": { "fmax": 1000, "fmin": 800, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1_2" } ], "location": { "fmax": 1400, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript1" }], "location": { "fmax": 1500, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```



#### set_longest_orf


Calculate the longest ORF for a transcript. The only element in the `features` array should be the transcript to process. Only the `uniquename` field needs to be set for the transcript. Returns the transcript's parent gene.

Request: `/annotationEditor/setLongestOrf`

```
{ "features": [{"uniquename": "transcript"}] }
```


Response:

``` 
{"features": [{ "children": [{ "children": [ { "location": { "fmax": 693, "fmin": 638, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1" }, { "location": { "fmax": 2628, "fmin": 2392, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon3" }, { "location": { "fmax": 2628, "fmin": 638, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "CDS" }, "uniquename": "transcript-CDS" }, { "location": { "fmax": 2223, "fmin": 849, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2" } ], "location": { "fmax": 2628, "fmin": 638, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" }], "location": { "fmax": 2735, "fmin": 0, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```


#### add_exon


Add exon(s) to a transcript. The first element of the `features` array should be the transcript to which the exon(s) will be added to. Each subsequent feature will be an exon. Merges overlapping exons. Returns the parent gene of the transcript.

Request: `/annotationEditor/addExon`

``` 
{ "features": [ {"uniquename": "transcript"}, { "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1" }, { "location": { "fmax": 600, "fmin": 400, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2" }, { "location": { "fmax": 1000, "fmin": 500, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon3" } ] }
```

Response:

``` 
{"features": [{ "children": [{ "children": [ { "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1" }, { "location": { "fmax": 1000, "fmin": 400, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2" } ], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" }], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```



#### delete_exon


Delete an exon from a transcript. If there are no exons left on the transcript, the transcript is deleted from the parent gene. The first element of the `features` array should be the transcript to which the exon(s) will be deleted from. Each subsequent feature will be an exon. Returns the parent gene of the transcript.

Request: `/annotationEditor/deleteExon`

``` 
{ "features": [ {"uniquename": "transcript"}, {"uniquename": "exon1"} ] }
```

Response:

``` 
{"features": [{ "children": [{ "children": [{ "location": { "fmax": 1000, "fmin": 400, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon2" }], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" }], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```


#### merge_exons


Merge exons. The `features` array should contain two exons. Each exon only requires `uniquename` to be set. Returns the parent gene.

Request: `/annotationEditor/mergeExons`

``` 
{ "features": [ {"uniquename": "exon1"}, {"uniquename": "exon2"} ] }
```

Response:

``` 
{"features": [{ "children": [{ "children": [{ "location": { "fmax": 500, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1" }], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" }], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```


#### split_exon


Splits the exon, creating two exons, the left one ends at at the new fmax and the right one starts at the new fmin. There should only be one element in the `features` array, the exon to be split. The exon must have `uniquename`, `location.fmin`, and `location.fmax` set. Returns the parent gene.

Request: `/annotationEditor/splitExon`

``` 
{ "features": [{ "location": { "fmax": 200, "fmin": 300 }, "uniquename": "exon1" }] }
```

Response:

``` 
{"features": [{ "children": [{ "children": [ { "location": { "fmax": 500, "fmin": 300, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1-right" }, { "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1-left" } ], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" }], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```


#### split_transcript


Split a transcript between the two exons. One transcript will contain all exons from the leftmost exon up to leftExon and the other will contain all exons from rightExon to the rightmost exon. The `features` array should contain two features, the first one for the left exon and the xecond one for the right exon. Exon's only need to have their `uniquename` fields set. Returns the parent gene.

Request: `/annotationEditor/splitTranscript`

``` 
{ "features": [ {"uniquename": "exon1-left"}, {"uniquename": "exon1-right"} ] }
```

Response:

``` 
{"features": [{ "children": [ { "children": [{ "location": { "fmax": 500, "fmin": 300, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1-right" }], "location": { "fmax": 1000, "fmin": 300, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript-split" }, { "children": [{ "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "exon" }, "uniquename": "exon1-left" }], "location": { "fmax": 200, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "transcript" }, "uniquename": "transcript" } ], "location": { "fmax": 1000, "fmin": 100, "strand": 1 }, "type": { "cv": {"name": "SO"}, "name": "gene" }, "uniquename": "gene" }]}
```


#### add_sequence_alteration


Add sequence alteration(s). Each element of the `features` array should be an alteration feature (e.g. substitution, insertion, deletion). Each alteration needs to have `location`, `type`, `uniquename`, and `residues` set. Returns the newly added sequence alteration features.

Request: `/annotationEditor/addSequenceAlteration`

``` 
{ "features": [{ "location": { "fmax": 642, "fmin": 641, "strand": 1 }, "residues": "T", "type": { "cv": {"name": "SO"}, "name": "substitution" }, "uniquename": "substitution1" }] }
```

Response:

``` 
{"features": [{ "location": { "fmax": 642, "fmin": 641, "strand": 1 }, "residues": "T", "type": { "cv": {"name": "SO"}, "name": "substitution" }, "uniquename": "substitution1" }]}
```


#### delete_sequence_alteration


Delete sequence alteration(s). Each feature only requires `uniquename` to be set. Returns an empty `features` array.

Request: `/annotationEditor/deleteSequenceAlteration`

``` 
{ "features": [ {"uniquename": "substitution1"}, {"uniquename": "substitution2"} ] }
```

Response:
 
``` 
{"features": []}
```


#### get_sequence_alterations

Get all sequence alterations. Returns an array of alterations.

Request: `/annotationEditor/getSequenceAlterations`

``` 
{ }
```

Response:

``` 
{
    "features": [{
        "location": { "fmax": 701, "fmin": 644, "strand": 1 },
        "residues": "ATT",
        "type": { "cv": {"name": "SO"}, "name": "insertion" },
        "uniquename": "insertion1"
    }, {
        "location": { "fmax": 2301, "fmin": 2300, "strand": 1 },
        "residues": "CCC", "type": { "cv": {"name": "SO"}, "name": "insertion" }, 
        "uniquename": "insertion2"
    }, {
        "location": { "fmax": 639, "fmin": 638, "strand": 1 },
        "residues": "C",
        "type": { "cv": {"name": "SO"}, "name": "substitution" },
        "uniquename": "substitution3"
    }]
}
```




#### add_attribute


Add attributes to a feature

Request: `/annotationEditor/addAttribute`

``` 
{ "features":[{ "non_reserved_properties":[{ "tag":"attributeName", "value":"attributeValue" }], "uniquename":"bc7a1c02-3503-416f-97f4-70489e6477b0" }] }
```

Response:

``` 
{ "features":[{ "id":207387, "date_creation":1438616294696, "location":{ "id":207388, "fmin":97044, "strand":1, "fmax":97323 }, "sequence":"Group1.14", "name":"GB51850-RA", "owner":"user@email.com", "children":[{ "id":207389, "date_creation":1438616294686, "location":{ "id":207390, "fmin":97044, "strand":1, "fmax":97323 }, "sequence":"Group1.14", "parent_type":{ "name":"gene", "cv":{"name":"sequence"} }, "name":"GB51850-RA-00001", "owner":"user@email.com", "children":[{ "id":207391, "date_creation":1438616294661, "location":{ "id":207392, "fmin":97044, "strand":1, "fmax":97323 }, "sequence":"Group1.14", "parent_type":{ "name":"mRNA", "cv":{"name":"sequence"} }, "name":"986735ff-d666-416f-8246-aba7f9c30d66-exon", "owner":"None", "properties":[{ "value":"None", "type":{ "name":"owner", "cv":{"name":"feature_property"} } }], "uniquename":"986735ff-d666-416f-8246-aba7f9c30d66", "type":{ "name":"exon", "cv":{"name":"sequence"} }, "date_last_modified":1438616294716, "parent_id":"2fe372e5-3ea6-4ef1-83af-747be8473ef3" }, { "id":207394, "date_creation":1438616294678, "location":{ "id":207395, "fmin":97044, "strand":1, "fmax":97323}, "sequence":"Group1.14", "parent_type":{ "name":"mRNA", "cv":{"name":"sequence"} }, "name":"a200b8e2-7a1f-44b4-8ec3-2cc7fe4e9981-CDS", "owner":"None", "properties":[{ "value":"None", "type":{ "name":"owner", "cv":{"name":"feature_property"} }}], "uniquename":"a200b8e2-7a1f-44b4-8ec3-2cc7fe4e9981", "type":{ "name":"CDS", "cv":{"name":"sequence"} }, "date_last_modified":1438616294718, "parent_id":"2fe372e5-3ea6-4ef1-83af-747be8473ef3" }], "properties":[{ "value":"user@email.com", "type":{ "name":"owner", "cv":{"name":"feature_property"} }}], "uniquename":"2fe372e5-3ea6-4ef1-83af-747be8473ef3", "type":{ "name":"mRNA", "cv":{"name":"sequence"} }, "date_last_modified":1438616294838, "parent_id":"bc7a1c02-3503-416f-97f4-70489e6477b0" }], "properties":[{ "value":"attributeName", "type":{ "cv":{"name":"feature_property"} }}, {"value":"attributeValue", "type":{ "cv":{"name":"feature_property"} }}, { "value":"user@email.com", "type":{ "name":"owner", "cv":{ "name":"feature_property"} }}], "uniquename":"bc7a1c02-3503-416f-97f4-70489e6477b0", "type":{ "name":"gene", "cv":{"name":"sequence"} }, "date_last_modified":1438616294836} ] }
```

#### add_comments


Add comments to a feature

Request: `/annotationEditor/addComments`

``` 
{ "features":[{ "uniquename":"bc7a1c02-3503-416f-97f4-70489e6477b0", "comments":["This is a comment"] }] }
```

Response:

``` 
{ "features":[{ "id":207389, "date_creation":1438616294686, "location":{ "id":207390, "fmin":97044, "strand":1, "fmax":97323 }, "sequence":"Group1.14", "parent_type":{ "name":"gene", "cv":{"name":"sequence"} }, "name":"GB51850-RA-00001", "owner":"user@email.com", "children":[{ "id":207391, "date_creation":1438616294661, "location":{ "id":207392, "fmin":97044, "strand":1, "fmax":97323}, "sequence":"Group1.14", "parent_type":{ "name":"mRNA", "cv":{"name":"sequence"} }, "name":"986735ff-d666-416f-8246-aba7f9c30d66-exon", "owner":"None", "properties":[{ "value":"None", "type":{ "name":"owner", "cv":{"name":"feature_property"} }}], "uniquename":"986735ff-d666-416f-8246-aba7f9c30d66", "type":{ "name":"exon", "cv":{"name":"sequence"} }, "date_last_modified":1438616294716, "parent_id":"2fe372e5-3ea6-4ef1-83af-747be8473ef3" }, {"id":207394, "date_creation":1438616294678, "location":{ "id":207395, "fmin":97044, "strand":1, "fmax":97323 }, "sequence":"Group1.14", "parent_type":{ "name":"mRNA", "cv":{"name":"sequence"} }, "name":"a200b8e2-7a1f-44b4-8ec3-2cc7fe4e9981-CDS", "owner":"None", "properties":[{ "value":"None", "type":{ "name":"owner", "cv":{ "name":"feature_property" }}}], "uniquename":"a200b8e2-7a1f-44b4-8ec3-2cc7fe4e9981", "type":{ "name":"CDS", "cv":{"name":"sequence"} }, "date_last_modified":1438616294718, "parent_id":"2fe372e5-3ea6-4ef1-83af-747be8473ef3" }], "properties":[{ "value":"user@email.com", "type":{ "name":"owner", "cv":{ "name":"feature_property" }}}], "uniquename":"2fe372e5-3ea6-4ef1-83af-747be8473ef3", "type":{ "name":"mRNA", "cv":{ "name":"sequence" }}, "date_last_modified":1438616294838, "parent_id":"bc7a1c02-3503-416f-97f4-70489e6477b0" }]}
```


#### set_status


Set status for a feature to indicate annotation quality or annotation progress.

Request: `/annotationEditor/setStatus`

``` 
{ "features":[{ "status":"Verification Needed", "uniquename":"bc7a1c02-3503-416f-97f4-70489e6477b0" }] }
```

Response:

``` 
{ "features": [{ "id": 207389, "date_creation": 1438616294686, "location": { "id": 207390, "fmin": 97044, "strand": 1, "fmax": 97323 }, "sequence": "Group1.14", "parent_type": { "name": "gene", "cv": {"name": "sequence"} }, "name": "GB51850-RA-00001", "owner": "user@email.com", "children": [{ "id": 207391, "date_creation": 1438616294661, "location": { "id": 207392, "fmin": 97044, "strand": 1, "fmax": 97323 }, "sequence": "Group1.14", "parent_type": { "name": "mRNA", "cv": {"name": "sequence"} }, "name": "986735ff-d666-416f-8246-aba7f9c30d66-exon", "owner": "None", "properties": [{ "value": "None", "type": { "name": "owner", "cv": {"name": "feature_property"} }}], "uniquename": "986735ff-d666-416f-8246-aba7f9c30d66", "type": { "name": "exon", "cv": {"name": "sequence"} }, "date_last_modified": 1438616294716, "parent_id": "2fe372e5-3ea6-4ef1-83af-747be8473ef3" }, { "id": 207394, "date_creation": 1438616294678, "location": { "id": 207395, "fmin": 97044, "strand": 1, "fmax": 97323 }, "sequence": "Group1.14", "parent_type": { "name": "mRNA", "cv": {"name": "sequence"} }, "name": "a200b8e2-7a1f-44b4-8ec3-2cc7fe4e9981-CDS", "owner": "None", "properties": [{ "value": "None", "type": { "name": "owner", "cv": {"name": "feature_property"} }}], "uniquename": "a200b8e2-7a1f-44b4-8ec3-2cc7fe4e9981", "type": { "name": "CDS", "cv": {"name": "sequence"} }, "date_last_modified": 1438616294718, "parent_id": "2fe372e5-3ea6-4ef1-83af-747be8473ef3" }], "properties": [{ "value": "user@email.com", "type": { "name": "owner", "cv": {"name": "feature_property"} }}], "uniquename": "2fe372e5-3ea6-4ef1-83af-747be8473ef3", "type": { "name": "mRNA", "cv": {"name": "sequence"} }, "date_last_modified": 1438616294838, "parent_id": "bc7a1c02-3503-416f-97f4-70489e6477b0" }] }
```


### IOServiceController

All JSON requests need to define: `operation` field, which defines the operation being


#### write

Can write to a file or to http output stream

Request: `/IOService/write`

Example

``` 
{ "adapter": "GFF3", "tracks": ["scf111111","scf111112"], "output": "text" }
```

Parameters:

* `adapter` 'GFF3','FASTA', 'Chado' ('Chado' not yet supported in 2.x)
* `tracks` an array of tracks / reference sequences, e.g., ["scf111111","scf111112"])
* `output` can be `file` or `plain`
* `format` can be `gzip` or `text`


#### download

This is used to retrieve the file once the write operation was initialized using output: file.


Parameters:

* `uuid` a UUID returned by the /IOService/write operation
* `exportType` the exportType is returned by the /IOService/write operation
* `seqType` the seqType is returned by the /IOService/write operation
* `format` the format is returned by the /IOService/write operation

Example:

An example script is used in the [get_gff3.groovy script](https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/get_gff3.groovy).


### UserController

User specific operations are restricted to users with administrator permissions.

#### create_user

Request: `/user/createUser`

``` 
{ firstName:"Bob", lastName:"Smith", username:"bob@admin.gov", password:"supersecret" }
```

Response: `{}`

#### delete_user

Request: `/user/deleteUser` 

``` 
{ userToDelete:"bob@admin.gov" }
```

Conversely, userId can also be passed in with the database id.

Response: `{}`


