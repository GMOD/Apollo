<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="en-US" xml:lang="en-US" xmlns="http://www.w3.org/1999/xhtml" xmlns="http://www.w3.org/1999/html">

<head>
    <meta name="layout" content="main">
    <title>Apollo Web Services API</title>
    <asset:stylesheet src="web_api_stylesheet"/>
    %{--<asset:image src="webapollo_favicon.ico"/>--}%
</head>

<body>
<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        %{--<li><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></li>--}%
    </ul>
</div>

<div class="section" id="login">

    <h3>Apollo Web Service API</h3>

    <p>
        The Apollo web service API is fully JSON based, to easily interact with JavaScript. Both the request and
        response JSON are feature information are based on the Chado schema.  We have provided numerous
        <a href="https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/">scripting examples</a> utilizing
    web services in addition to utilizing them internally.
    </p>


    <h3>Requests</h3>

    <p>
        For a given apollo server url (e.g., <code>https://myawesomewebsite.edu/apollo</code>), we need an appropriate website.
    All JSON requests need:
    </p>

    <ul>
        <li>To be addressed to the proper url (e.g., to get features <code>/annotationEditor/getFeatures</code> ->
            <code>https://myawesomewebsite.edu/apollo/annotationEditor/getFeatures</code>)</li>
        <li><code>username</code> in the JSON object</li>
        <li><code>password</code> in the JSON object</li>
        <li><code>organism</code> (optional) common name (shown in organism panel) in the JSON object for feature related operations
        </li>
        <li><code>track/sequence</code> (optional) reference sequence name (shown in sequence panel / genomic browse) in the JSON object for most feature operations
        </li>
    </ul>

    <p>
        <code>uniquname</code> is a common parameter as well. This is a <a
            href="https://docs.oracle.com/javase/7/docs/api/java/util/UUID.html">UUID</a>
        used to guarantee a unique ID across systems and not the name/symbol of any feature..
    </p>

    <h4>Errors</h4>
    If an error has occurred, a proper HTTP error code (most likely 400 or 500) and an error message.
    is returned, in JSON format:

    <div class="code">{
    "error": "mergeExons(): Exons must be in the same strand"
    }
    </div>

    <h4>Additional Notes</h4>

    <p>
        If you are sending password you care about over the wire (even if not using web services) it is <strong>highly recommended</strong>  that you use https (which adds encryption ssl) instead of http.
    </p>


    %{--<h4>Request</h4>--}%

    %{--<p>--}%
    %{--The URL for login is:--}%

    %{--<div class="code">http://$server:$port/ApolloWeb/Login</div>--}%
    %{--where <code>Rserver</code> is the server name and <code>$port</code> is the server port.--}%

    %{--<p>--}%
    %{--For example:--}%

    %{--<div class="code">curl -b cookies.txt -c cookies.txt -e "http://localhost:8080" \--}%
    %{---H "Content-Type:application/json" -d "{'username': 'demo', 'password': 'demo'}"--}%
    %{--"http://localhost:8080/apollo/Login?operation=login"--}%
    %{--</div>--}%
    %{--</p>--}%


    %{--<p>--}%
    %{--Login expects two parameters: <code>username</code> and <code>password</code>. There currently isn't--}%
    %{--any real user authentication implemented, so <code>username</code> and <code>password</code> should be set to--}%
    %{--<code>foo</code> and <code>bar</code> respectively.--}%
    %{--</p>--}%

    %{--<h4>Response</h4>--}%

    %{--<p>--}%
    %{--Login will return a JSON containing the <code>session-id</code> for the user. This is needed if the user's--}%
    %{--browser does not support cookies (or is turned off), in which case the <code>session-id</code> should be--}%
    %{--appended to all subsequent requests as <code>jsessionid=session-id</code> as an URL parameter.--}%

    %{--<div class="code">{"session-id":"43FBA5B967595D260A1C0E6B7052C7A1"}--}%
    %{--</div>--}%
</div>

<div class="section" id="feature">
    <h3>Feature Object</h3>

    <p>
        Most requests and responses will contain an array of <code>feature</code> JSON objects named
        <code>features</code>.
    The <code>feature</code> object is based on the Chado <code>feature</code>, <code>featureloc</code>,
        <code>cv</code>,
    and <code>cvterm</code> tables.

    <div class="code">{
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
    </div>
    where:
    <ul>
        <li><code>residues</code> - A sequence of alphabetic characters representing biological residues (nucleic acids,
        amino acids) [string]
        </li>
        <li><code>type.cv.name</code> - The name of the ontology [string]</li>
        <li><code>type.name</code> - The name for the cvterm [string]</li>
        <li><code>location.fmax</code> - The rightmost/maximal intrabase boundary in the linear range [integer]</li>
        <li><code>location.fmin</code> - The leftmost/minimal intrabase boundary in the linear range [integer]</li>
        <li><code>strand</code> - The orientation/directionality of the location. Should be 0, -1 or +1 [integer]</li>
        <li><code>uniquename</code> - The unique name for a feature [string]</li>
        <li><code>children</code> - Array of child feature objects [array]</li>
        <li><code>properties</code> - Array of properties (including frameshifts for transcripts) [array]</li>
    </ul>
    Note that different operations will require different fields to be set (which will be elaborated upon in each
    operation section).
</p>

</div>

<div class="section" id="operations">
    <h3>Operations</h3>


    <h4>add_organism</h4>

    <p>
        Adds an organism to the database.
        An example using this script <a
            href="https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/add_organism.groovy">add_organism.groovy</a>.
    </p>

    <p>
        Request:  <code>/organism/addOrganism</code>
    </p>

    <div class="code">
        {
        "directory": "/opt/apollo/myanimal/jbrowse/data",
        "username": "bob@admin.gov",
        "password": "password"
        "blatdb": "/opt/apollo/myanimal/myanimal.2bit",
        "genus": "My",
        "species": "animal",
        }
    </div>

    <p>
        Response Status 200:
    </p>

    <div class="code">
        {}
    </div>

    <h4>get_sequences_for_organism</h4>
    
    <p>
        Gets all sequence names for a given organism, that has annotations.
        An example using this operation is <a href="https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/groovy/migrate_annotations.groovy">migrate_annotations.groovy</a>.
    </p>
    
    <p>
        Request: <code>/organism/getSequencesForOrganism</code>
    </p>
    
    <div class="code">
        {
        "username": "bob@admin.gov",
        "password": "password",
        "organism": "organism_common_name"
        }
    </div>
    
    <p>
        Response Status 200:
    </p>
    
    <div class="code">
        {
        "username": "bob@admin.gov",
        "sequences": [ "chr1", "chr4", "chr11" ],
        "organism": "organism_common_name"
        }
    </div>
    
    <h4>add_feature</h4>

    <inp>
        Add a top level feature. Returns feature just added.

    </inp>

    <p>
        Request: <code>/annotationEditor/addFeature</code>
    </p>

    <div class="code">{
    "features": [{
    "location": {
    "fmax": 2735,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "location": {
    "fmax": 2735,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>delete_feature</h4>

    <p>
        Delete feature(s) from the session. Each feature only requires <code>uniquename</code> to be set. Returns
    an empty <code>features</code> array.
    </p>

    <p>
        Request: <code>/annotationEditor/deleteFeature</code>
    </p>

    <div class="code">{
    "features": [{"uniquename": "gene"}],
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": []}
    </div>

    <h4>get_features</h4>

    <p>
        Get all top level features.
    </p>

    <p>
        Request: <code>/annotationEditor/getFeatures</code>
    </p>

    <div class="code">
        { }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "location": {
    "fmax": 2735,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>add_transcript</h4>

    <p>
        Add transcript(s) to a gene. The first element of the <code>features</code> array should be the gene to add
    the transcript(s) to, with each subsequent feature being a transcript to be added. The gene feature only
    requires the <code>uniquename</code> field to be set. Returns the gene which the transcript was added to.
    </p>

    <p>
        Request: <code>/annotationEditor/addTranscript</code>
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "gene"},
    {
    "location": {
    "fmax": 2628,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "location": {
    "fmax": 2628,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }],
    "location": {
    "fmax": 2735,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>duplicate_transcript</h4>

    <p>
        Duplicate a transcript. Only the first transcript in the <code>features</code> array is processed. The transcript
    feature only requires <code>uniquename</code> to be set. Returns the parent gene of the transcript.
    </p>

    <p>
        Request: <code>/annotationEditor/duplicateTranscript</code>
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript"}]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [
    {
    "location": {
    "fmax": 2628,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript-copy"
    },
    {
    "location": {
    "fmax": 2628,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }
    ],
    "location": {
    "fmax": 2735,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>merge_transcripts</h4>

    <p>
        Merge two transcripts together. Only the transcripts in the first and second positions in the <code>features</code>
        array
        are processed. The transcript features only requires <code>uniquename</code> to be set. If the two transcripts
    belong
    to different genes, the parent genes are merged as well. Returns the parent gene of the merged transcripts.
    </p>

    <p>
        Request: <code>/annotationEditor/mergeTranscripts</code>
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "transcript1"},
    {"uniquename": "transcript2"}
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [
    {
    "children": [
    {
    "location": {
    "fmax": 2400,
    "fmin": 2000,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2_1"
    },
    {
    "location": {
    "fmax": 700,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1_1"
    },
    {
    "location": {
    "fmax": 1500,
    "fmin": 1000,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1_2"
    },
    {
    "location": {
    "fmax": 2700,
    "fmin": 2500,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2_2"
    }
    ],
    "location": {
    "fmax": 2700,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript1"
    }
    ],
    "location": {
    "fmax": 2700,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene1"
    }]}
    </div>

    <h4>set_translation_start</h4>

    <p>
        Set the CDS start and end in a transcript. The transcript feature only needs to have the <code>uniquename</code>
        field set. The JSON transcript must contain a CDS feature, which will contain the new CDS boundaries. Other
        children of the transcript will be ignored. Returns the parent gene of the transcript.
    </p>

    <p>
        Request: <code>/annotationEditor/setTranslationStart</code>
    </p>

    <div class="code">{
    "features": [{
    "children": [{
    "location": {
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "CDS"
    },
    "uniquename": "cds"
    }],
    "uniquename": "transcript1"
    }]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "children": [
    {
    "location": {
    "fmax": 700,
    "fmin": 500,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2_1"
    },
    {
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1_1"
    },
    {
    "location": {
    "fmin": 100,
    "fmin": 1400,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "CDS"
    },
    "uniquename": "transcript1-CDS"
    },
    {
    "location": {
    "fmax": 1400,
    "fmin": 1200,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2_3"
    },
    {
    "location": {
    "fmax": 1000,
    "fmin": 800,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1_2"
    }
    ],
    "location": {
    "fmax": 1400,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript1"
    }],
    "location": {
    "fmax": 1500,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>set_translation_end</h4>

    <p>
        Set the CDS end in a transcript. The transcript feature only needs to have the <code>uniquename</code>
        field set. The JSON transcript must contain a CDS feature, which will contain the new CDS boundaries. Other
        children of the transcript will be ignored. Returns the parent gene of the transcript.
    </p>

    <p>
        Request: <code>/annotationEditor/setTranslationEnd</code>
    </p>

    <div class="code">{
    "features": [{
    "children": [{
    "location": {
    "fmax": 1300,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "CDS"
    },
    "uniquename": "cds"
    }],
    "uniquename": "transcript1"
    }]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "children": [
    {
    "location": {
    "fmax": 700,
    "fmin": 500,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2_1"
    },
    {
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1_1"
    },
    {
    "location": {
    "fmax": 1300,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "CDS"
    },
    "uniquename": "transcript1-CDS"
    },
    {
    "location": {
    "fmax": 1400,
    "fmin": 1200,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2_3"
    },
    {
    "location": {
    "fmax": 1000,
    "fmin": 800,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1_2"
    }
    ],
    "location": {
    "fmax": 1400,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript1"
    }],
    "location": {
    "fmax": 1500,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>


    <h4>set_longest_orf</h4>

    <p>
        Calculate the longest ORF for a transcript. The only element in the <code>features</code> array should
    be the transcript to process. Only the <code>uniquename</code> field needs to be set for the transcript.
    Returns the transcript's parent gene.
    </p>

    <p>
        Request: <code>/annotationEditor/setLongestOrf</code>
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript"}]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">Response for operation: set_longest_orf
    {"features": [{
    "children": [{
    "children": [
    {
    "location": {
    "fmax": 693,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1"
    },
    {
    "location": {
    "fmax": 2628,
    "fmin": 2392,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon3"
    },
    {
    "location": {
    "fmax": 2628,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "CDS"
    },
    "uniquename": "transcript-CDS"
    },
    {
    "location": {
    "fmax": 2223,
    "fmin": 849,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2"
    }
    ],
    "location": {
    "fmax": 2628,
    "fmin": 638,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }],
    "location": {
    "fmax": 2735,
    "fmin": 0,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>add_exon</h4>

    <p>
        Add exon(s) to a transcript. The first element of the <code>features</code> array should be the transcript
    to which the exon(s) will be added to. Each subsequent feature will be an exon. Merges overlapping exons.
    Returns the parent gene of the transcript.
    </p>

    <p>
        Request: <code>/annotationEditor/addExon</code>
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "transcript"},
    {
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1"
    },
    {
    "location": {
    "fmax": 600,
    "fmin": 400,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2"
    },
    {
    "location": {
    "fmax": 1000,
    "fmin": 500,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon3"
    }
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "children": [
    {
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1"
    },
    {
    "location": {
    "fmax": 1000,
    "fmin": 400,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2"
    }
    ],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>delete_exon</h4>

    <p>
        Delete an exon from a transcript. If there are no exons left on the transcript, the transcript
        is deleted from the parent gene. The first element of the <code>features</code> array should be the transcript
    to which the exon(s) will be deleted from. Each subsequent feature will be an exon. Returns the parent gene of
    the transcript.
    </p>

    <p>
        Request: <code>/annotationEditor/deleteExon</code>
    </p>

    <div class="code">}{
    "features": [
    {"uniquename": "transcript"},
    {"uniquename": "exon1"}
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "children": [{
    "location": {
    "fmax": 1000,
    "fmin": 400,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon2"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>merge_exons</h4>

    <p>
        Merge exons. The <code>features</code> array should contain two exons. Each exon only requires
        <code>uniquename</code> to be set. Returns the parent gene.
    </p>

    <p>
        Request: <code>/annotationEditor/mergeExons</code>
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "exon1"},
    {"uniquename": "exon2"}
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "children": [{
    "location": {
    "fmax": 500,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>split_exon</h4>

    <p>
        Splits the exon, creating two exons, the left one ends at at the new fmax and the right one starts at the
        new fmin. There should only be one element in the <code>features</code> array, the exon to be split.
    The exon must have <code>uniquename</code>, <code>location.fmin</code>, and <code>location.fmax</code>
        set. Returns the parent gene.
    </p>

    <p>
        Request: <code>/annotationEditor/splitExon</code>
    </p>

    <div class="code">{
    "features": [{
    "location": {
    "fmax": 200,
    "fmin": 300
    },
    "uniquename": "exon1"
    }]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [{
    "children": [
    {
    "location": {
    "fmax": 500,
    "fmin": 300,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1-right"
    },
    {
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1-left"
    }
    ],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>split_transcript</h4>

    <p>
        Split a transcript between the two exons. One transcript will contain all exons from the leftmost
        exon up to leftExon and the other will contain all exons from rightExon to the rightmost exon.
        The <code>features</code> array should contain two features, the first one for the left exon and
    the xecond one for the right exon. Exon's only need to have their <code>uniquename</code> fields
    set. Returns the parent gene.
    </p>

    <p>
        Request: <code>/annotationEditor/splitTranscript</code>
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "exon1-left"},
    {"uniquename": "exon1-right"}
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "children": [
    {
    "children": [{
    "location": {
    "fmax": 500,
    "fmin": 300,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1-right"
    }],
    "location": {
    "fmax": 1000,
    "fmin": 300,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript-split"
    },
    {
    "children": [{
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "exon"
    },
    "uniquename": "exon1-left"
    }],
    "location": {
    "fmax": 200,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "transcript"
    },
    "uniquename": "transcript"
    }
    ],
    "location": {
    "fmax": 1000,
    "fmin": 100,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "gene"
    },
    "uniquename": "gene"
    }]}
    </div>

    <h4>add_sequence_alteration</h4>

    <p>
        Add sequence alteration(s). Each element of the <code>features</code> array should be an alteration feature
    (e.g. substitution, insertion, deletion). Each alteration needs to have <code>location</code>, <code>type</code>,
        <code>uniquename</code>, and <code>residues</code> set. Returns the newly added sequence alteration features.
    </p>

    <p>
        Request: <code>/annotationEditor/addSequenceAlteration</code>
    </p>

    <div class="code">{
    "features": [{
    "location": {
    "fmax": 642,
    "fmin": 641,
    "strand": 1
    },
    "residues": "T",
    "type": {
    "cv": {"name": "SO"},
    "name": "substitution"
    },
    "uniquename": "substitution1"
    }]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "location": {
    "fmax": 642,
    "fmin": 641,
    "strand": 1
    },
    "residues": "T",
    "type": {
    "cv": {"name": "SO"},
    "name": "substitution"
    },
    "uniquename": "substitution1"
    }]}
    </div>

    <h4>delete_sequence_alteration</h4>

    <p>
        Delete sequence alteration(s). Each feature only requires <code>uniquename</code> to be set. Returns
    an empty <code>features</code> array.
    </p>

    <p>
        Request: <code>/annotationEditor/deleteSequenceAlteration</code>
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "substitution1"},
    {"uniquename": "substitution2"}
    ]
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": []}
    </div>

    <h4>get_sequence_alterations</h4>

    <p>
        Get all sequence alterations. Returns an array of alterations.
    </p>

    <p>
        Request: <code>/annotationEditor/getSequenceAlterations</code>
    </p>

    <div class="code">
        { }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [
    {
    "location": {
    "fmax": 701,
    "fmin": 644,
    "strand": 1
    },
    "residues": "ATT",
    "type": {
    "cv": {"name": "SO"},
    "name": "insertion"
    },
    "uniquename": "insertion1"
    },
    {
    "location": {
    "fmax": 2301,
    "fmin": 2300,
    "strand": 1
    },
    "residues": "CCC",
    "type": {
    "cv": {"name": "SO"},
    "name": "insertion"
    },
    "uniquename": "insertion2"
    },
    {
    "location": {
    "fmax": 639,
    "fmin": 638,
    "strand": 1
    },
    "residues": "C",
    "type": {
    "cv": {"name": "SO"},
    "name": "substitution"
    },
    "uniquename": "substitution3"
    }
    ]}
    </div>

    %{--<h4>get_residues_with_alterations</h4>--}%

    %{--<p>--}%
    %{--Get the residues for feature(s) with any alterations. Only <code>uniquename</code> needs to be set for each--}%
    %{--feature. Returns the requested feature(s), stripped down to only their <code>uniquename</code> and--}%
    %{--<code>residues</code>.--}%
    %{--</p>--}%

    %{--<p>--}%
    %{--Request:--}%
    %{--</p>--}%

    %{--<div class="code">{--}%
    %{--"features": [{"uniquename": "transcript-CDS"}],--}%
    %{--"operation": "get_residues_with_alterations"--}%
    %{--}--}%
    %{--</div>--}%

    %{--<p>--}%
    %{--Response:--}%
    %{--</p>--}%

    %{--<div class="code">{"features": [{--}%
    %{--"residues": "ATGTATCAGTACGGAAGA...",--}%
    %{--"uniquename": "transcript-CDS"--}%
    %{--}]}--}%
    %{--</div>--}%

    %{--<h4>add_frameshift</h4>--}%

    %{--<p>--}%
    %{--Add a frameshift to the transcript. The transcript must be the first element in the <code>features</code> array and--}%
    %{--it must contain a <code>properties</code> array, with each element being a frameshift. Returns the transcript's--}%
    %{--parent gene.--}%
    %{--</p>--}%

    %{--<p>--}%
    %{--Request:--}%
    %{--</p>--}%

    %{--<div class="code">{--}%
    %{--"features": [{--}%
    %{--"properties": [{--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "plus_1_frameshift"--}%
    %{--},--}%
    %{--"value": "100"--}%
    %{--}],--}%
    %{--"uniquename": "transcript"--}%
    %{--}],--}%
    %{--"operation": "add_frameshift"--}%
    %{--}--}%
    %{--</div>--}%

    %{--<p>--}%
    %{--Response:--}%
    %{--</p>--}%

    %{--<div class="code">{"features": [{--}%
    %{--"children": [{--}%
    %{--"children": [--}%
    %{--{--}%
    %{--"location": {--}%
    %{--"fmax": 693,--}%
    %{--"fmin": 638,--}%
    %{--"strand": 1--}%
    %{--},--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "exon"--}%
    %{--},--}%
    %{--"uniquename": "exon1"--}%
    %{--},--}%
    %{--{--}%
    %{--"location": {--}%
    %{--"fmax": 2628,--}%
    %{--"fmin": 2392,--}%
    %{--"strand": 1--}%
    %{--},--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "exon"--}%
    %{--},--}%
    %{--"uniquename": "exon3"--}%
    %{--},--}%
    %{--{--}%
    %{--"location": {--}%
    %{--"fmax": 2628,--}%
    %{--"fmin": 890,--}%
    %{--"strand": 1--}%
    %{--},--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "CDS"--}%
    %{--},--}%
    %{--"uniquename": "transcript-CDS"--}%
    %{--},--}%
    %{--{--}%
    %{--"location": {--}%
    %{--"fmax": 2223,--}%
    %{--"fmin": 849,--}%
    %{--"strand": 1--}%
    %{--},--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "exon"--}%
    %{--},--}%
    %{--"uniquename": "exon2"--}%
    %{--}--}%
    %{--],--}%
    %{--"location": {--}%
    %{--"fmax": 2628,--}%
    %{--"fmin": 638,--}%
    %{--"strand": 1--}%
    %{--},--}%
    %{--"properties": [{--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "plus_1_frameshift"--}%
    %{--},--}%
    %{--"value": "100"--}%
    %{--}],--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "transcript"--}%
    %{--},--}%
    %{--"uniquename": "transcript"--}%
    %{--}],--}%
    %{--"location": {--}%
    %{--"fmax": 2735,--}%
    %{--"fmin": 0,--}%
    %{--"strand": 1--}%
    %{--},--}%
    %{--"type": {--}%
    %{--"cv": {"name": "SO"},--}%
    %{--"name": "gene"--}%
    %{--},--}%
    %{--"uniquename": "gene"--}%
    %{--}]}--}%
    %{--</div>--}%

    %{--<h4>get_residues_with_frameshifts</h4>--}%

    %{--<p>--}%
    %{--Get the residues for feature(s) with any frameshifts. Only applicable to CDS features. Other features will return--}%
    %{--unmodified residues. Only <code>uniquename</code> needs to be set for each feature. Returns the requested--}%
    %{--feature(s), stripped down to only their <code>uniquename</code> and <code>residues</code>.--}%
    %{--</p>--}%

    %{--<p>--}%
    %{--Request:--}%
    %{--</p>--}%

    %{--<div class="code">{--}%
    %{--"features": [{"uniquename": "transcript-CDS"}],--}%
    %{--"operation": "get_residues_with_frameshifts"--}%
    %{--}--}%
    %{--</div>--}%

    %{--<p>--}%
    %{--Response:--}%
    %{--</p>--}%

    %{--<div class="code">{"features": [{--}%
    %{--"residues": "ATGTATCAGTACGGAAGA...",--}%
    %{--"uniquename": "transcript-CDS"--}%
    %{--}]}--}%
    %{--</div>--}%

    %{--<h4>get_residues_with_alterations_and_frameshifts</h4>--}%

    %{--<p>--}%
    %{--Get the residues for feature(s) with any alteration and frameshifts. Only <code>uniquename</code> needs to be set--}%
    %{--for each--}%
    %{--feature. Returns the requested feature(s), stripped down to only their <code>uniquename</code> and--}%
    %{--<code>residues</code>.--}%
    %{--</p>--}%

    %{--<p>--}%
    %{--Request:--}%
    %{--</p>--}%

    %{--<div class="code">{--}%
    %{--"features": [{"uniquename": "transcript-CDS"}],--}%
    %{--"operation": "get_residues_with_alterations_and_frameshifts"--}%
    %{--}--}%
    %{--</div>--}%

    %{--<p>--}%
    %{--Response:--}%
    %{--</p>--}%

    %{--<div class="code">{"features": [{--}%
    %{--"residues": "ATGTATCAGTACGGAAGA...",--}%
    %{--"uniquename": "transcript-CDS"--}%
    %{--}]}--}%
    %{--</div>--}%

</div>

<div class="section" id="ioservice">
    <h3>IO Service</h3>

    <p>
        All JSON requests need to define:
        <code>operation</code> field, which defines the operation being
    </p>

    <ul>
        <li>
            <div class="code">'operation' ('read' or 'write')</div>
        </li>
        <li>
            %{--<div class="code">'adapter' ('GFF3','FASTA','Chado')</div>--}%
            <div class="code">'adapter' ('GFF3','FASTA')</div>
        </li>
        <li>
            <div class="code">'tracks' (an array of tracks / reference sequences, e.g., ["Annotations-scf11","Annotations-BCD"])</div>
        </li>
        <li>
            <div class="code">'options' (e.g. output=file&format=gzip)</div>
        </li>
    </ul>

    <p>
        requested (read or write) is returned according to the options and the adapter chosen.
    </p>

    <p>
        Example:
    </p>

    <div class="code">
        curl -e "http://$hostname:$port" --data '{ operation: "write", adapter: "GFF3",
        tracks: ["Annotations-scf1117875582023"], options: "output=file&format=gzip",'username': '$username', 'password': '$password','organism':'$organism' }'
        http://$hostname:$port:8080/apollo/IOService
    </div>

</div>

<div class="section" id="userservice">
    <h3>User Web Service</h3>

    <p>
        User specific operations are restricted to users with administrator permissions.
    </p>

    <h4>create_user</h4>

    <p>
        Request: <code>/user/createUser</code>

        <code>
            {
            firstName:"Bob",
            lastName:"Smith",
            username:"bob@admin.gov",
            password:"supersecret"
            }
        </code>
    </p>
    <p>
        Response: <code>{}</code>
    </p>

    <h4>delete_user</h4>

    <p>
        Request: <code>/user/deleteUser</code>

        <code>
            {
            userToDelete:"bob@admin.gov"
            }
        </code>

        Conversely, userId can also be passed in with the database id.
    </p>
    <p>
        Response: <code>{}</code>
    </p>


</div>

</body>

</html>
