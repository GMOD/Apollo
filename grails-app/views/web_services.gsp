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
        response JSON are feature information are based on the Chado schema.
    </p>


    <h3>Login</h3>

    <p>
        To properly setup an user's editing session, the user needs to login to the web service. This is to prevent
        an user from just loading a page directly and making a request without prior authentication.
    </p>

    <h4>Request</h4>

    <p>
        The URL for login is:

    <div class="code">http://$server:$port/ApolloWeb/Login</div>
    where <code>Rserver</code> is the server name and <code>$port</code> is the server port.
</p>

    <p>
        For example:

    <div class="code">curl -b cookies.txt -c cookies.txt -e "http://localhost:8080" \
    -H "Content-Type:application/json" -d "{'username': 'demo', 'password': 'demo'}"
    "http://localhost:8080/apollo/Login?operation=login"
    </div>
</p>


    <p>
        Login expects two parameters: <code>username</code> and <code>password</code>. There currently isn't
    any real user authentication implemented, so <code>username</code> and <code>password</code> should be set to
        <code>foo</code> and <code>bar</code> respectively.
    </p>

    <h4>Response</h4>

    <p>
        Login will return a JSON containing the <code>session-id</code> for the user. This is needed if the user's
    browser does not support cookies (or is turned off), in which case the <code>session-id</code> should be
    appended to all subsequent requests as <code>jsessionid=session-id</code> as an URL parameter.

    <div class="code">{"session-id":"43FBA5B967595D260A1C0E6B7052C7A1"}
    </div>
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

    <p>
        All JSON requests need to contain a <code>operation</code> field, which defines the operation being
    requested. If an error has occurred, a proper HTTP error code (most likely 400) and an error message
    is returned, in JSON format:
    </p>

    <div class="code">{
    "error": "mergeExons(): Exons must be in the same strand"
    }
    </div>

    <h4>add_organism</h4>

    <p>
        Adds an organism to the database.  See "add_organism.groovy"
    </p>

    <p>
        Request:
    </p>

    <div class="code">
        {
        "operation": "add_organism",
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


    <h4>set_organism</h4>

    <p>
        Set the organism to be associated with this session. Returns the organism just set.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "operation": "set_organism",
    "organism": {
    "genus": "Foomus",
    "species": "barius"
    }
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{
    "genus": "Foomus",
    "species": "barius"
    }
    </div>

    <h4>get_organism</h4>

    <p>
        Get the organism associated with this session.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "operation": "set_organism",
    "organism": {
    "genus": "Foomus",
    "species": "barius"
    }
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{
    "genus": "Foomus",
    "species": "barius"
    }
    </div>

    <h4>set_source_feature</h4>

    <p>
        Set the source feature to be associated with this session. Returns the source feature just set.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "features": [{
    "residues": "ATATCTTTTCTCACAATCGTTG...",
    "type": {
    "cv": {"name": "SO"},
    "name": "chromosome"
    },
    "uniquename": "chromosome"
    }],
    "operation": "set_source_feature"
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "residues": "ATATCTTTTCTCACAATCGTTG...",
    "type": {
    "cv": {"name": "SO"},
    "name": "chromosome"
    },
    "uniquename": "chromosome"
    }]}
    </div>

    <h4>get_source_feature</h4>

    <p>
        Get the source feature associated with this session.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "operation": "get_source_feature"
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "residues": "ATATCTTTTCTCACAATCGTTG...",
    "type": {
    "cv": {"name": "SO"},
    "name": "chromosome"
    },
    "uniquename": "chromosome"
    }]}
    </div>

    <h4>add_feature</h4>

    <p>
        Add a top level feature. Returns feature just added.
    </p>

    <p>
        Request:
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
    }],
    "operation": "add_feature"
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
        Request:
    </p>

    <div class="code">{
    "features": [{"uniquename": "gene"}],
    "operation": "delete_feature"
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
        Request:
    </p>

    <div class="code">{
    "operation": "get_features"
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

    <h4>add_transcript</h4>

    <p>
        Add transcript(s) to a gene. The first element of the <code>features</code> array should be the gene to add
    the transcript(s) to, with each subsequent feature being a transcript to be added. The gene feature only
    requires the <code>uniquename</code> field to be set. Returns the gene which the transcript was added to.
    </p>

    <p>
        Request:
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
    ],
    "operation": "add_transcript"
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
        Request:
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript"}],
    "operation": "duplicate_transcript"
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
        Request:
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "transcript1"},
    {"uniquename": "transcript2"}
    ],
    "operation": "merge_transcripts"
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
        Request:
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
    }],
    "operation": "set_translation_start"
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
        Request:
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
    }],
    "operation": "set_translation_end"
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

    <h4>set_translation_ends</h4>

    <p>
        Set the CDS start and end in a transcript. The transcript feature only needs to have the <code>uniquename</code>
        field set. The JSON transcript must contain a CDS feature, which will contain the new CDS boundaries. Other
        children of the transcript will be ignored. Returns the parent gene of the transcript.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "features": [{
    "children": [{
    "location": {
    "fmax": 1400,
    "fmin": 200,
    "strand": 1
    },
    "type": {
    "cv": {"name": "SO"},
    "name": "CDS"
    },
    "uniquename": "cds"
    }],
    "uniquename": "transcript1"
    }],
    "operation": "set_translation_ends"
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
    "fmax": 1400,
    "fmin": 200,
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
        Request:
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript"}],
    "operation": "set_longest_orf"
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
        Request:
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
    ],
    "operation": "add_exon"
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
        Request:
    </p>

    <div class="code">}{
    "features": [
    {"uniquename": "transcript"},
    {"uniquename": "exon1"}
    ],
    "operation": "delete_exon"
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
        Request:
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "exon1"},
    {"uniquename": "exon2"}
    ],
    "operation": "merge_exons"
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
        Request:
    </p>

    <div class="code">{
    "features": [{
    "location": {
    "fmax": 200,
    "fmin": 300
    },
    "uniquename": "exon1"
    }],
    "operation": "split_exon"
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
        Request:
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "exon1-left"},
    {"uniquename": "exon1-right"}
    ],
    "operation": "split_transcript"
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
        Request:
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
    }],
    "operation": "add_sequence_alteration"
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
        Request:
    </p>

    <div class="code">{
    "features": [
    {"uniquename": "substitution1"},
    {"uniquename": "substitution2"}
    ],
    "operation": "delete_sequence_alteration"
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
        Request:
    </p>

    <div class="code">{
    "operation": "get_sequence_alterations"
    }
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

    <h4>get_residues_with_alterations</h4>

    <p>
        Get the residues for feature(s) with any alterations. Only <code>uniquename</code> needs to be set for each
    feature. Returns the requested feature(s), stripped down to only their <code>uniquename</code> and
        <code>residues</code>.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript-CDS"}],
    "operation": "get_residues_with_alterations"
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "residues": "ATGTATCAGTACGGAAGA...",
    "uniquename": "transcript-CDS"
    }]}
    </div>

    <h4>add_frameshift</h4>

    <p>
        Add a frameshift to the transcript. The transcript must be the first element in the <code>features</code> array and
    it must contain a <code>properties</code> array, with each element being a frameshift. Returns the transcript's
    parent gene.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "features": [{
    "properties": [{
    "type": {
    "cv": {"name": "SO"},
    "name": "plus_1_frameshift"
    },
    "value": "100"
    }],
    "uniquename": "transcript"
    }],
    "operation": "add_frameshift"
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
    "fmin": 890,
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
    "properties": [{
    "type": {
    "cv": {"name": "SO"},
    "name": "plus_1_frameshift"
    },
    "value": "100"
    }],
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

    <h4>get_residues_with_frameshifts</h4>

    <p>
        Get the residues for feature(s) with any frameshifts. Only applicable to CDS features. Other features will return
        unmodified residues. Only <code>uniquename</code> needs to be set for each feature. Returns the requested
    feature(s), stripped down to only their <code>uniquename</code> and <code>residues</code>.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript-CDS"}],
    "operation": "get_residues_with_frameshifts"
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "residues": "ATGTATCAGTACGGAAGA...",
    "uniquename": "transcript-CDS"
    }]}
    </div>

    <h4>get_residues_with_alterations_and_frameshifts</h4>

    <p>
        Get the residues for feature(s) with any alteration and frameshifts. Only <code>uniquename</code> needs to be set
    for each
    feature. Returns the requested feature(s), stripped down to only their <code>uniquename</code> and
        <code>residues</code>.
    </p>

    <p>
        Request:
    </p>

    <div class="code">{
    "features": [{"uniquename": "transcript-CDS"}],
    "operation": "get_residues_with_alterations_and_frameshifts"
    }
    </div>

    <p>
        Response:
    </p>

    <div class="code">{"features": [{
    "residues": "ATGTATCAGTACGGAAGA...",
    "uniquename": "transcript-CDS"
    }]}
    </div>

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
            <div class="code">'adapter' ('GFF3','FASTA','Chado')</div>
        </li>
        <li>
            <div class="code">'tracks' (an array of tracks, e.g., ["Annotations-scf11","Annotations-BCD"])</div>
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
        curl -b cookies.txt -c cookies.txt -e "http://$hostname:$port" -H "Content-Type:application/json" -d
        "{'username': '$username', 'password': '$password'}" "http://$hostname:$port/apollo/Login?operation=login"
    </div>

    <div class="code">
        curl -b cookies.txt -c cookies.txt -e "http://$hostname:$port" --data '{ operation: "write", adapter: "GFF3",
        tracks: ["Annotations-scf1117875582023"], options: "output=file&format=gzip" }'
        http://$hostname:$port:8080/apollo/IOService
    </div>

</div>

<div class="section" id="userservice">
    <h3>User Manager Service</h3>

    <p>
        All JSON requests need to define:
        <code>operation</code> field, which defines the operation being
    </p>
    <ul>
        <li>
            <div class="code">'operation' ('add_user', 'delete_user', 'set_permissions' )</div>
        </li>
    </ul>

    <p>
        requested (read or write) is returned according to the options and the adapter chosen.
    </p>

    <p>
        Example:
    </p>

    <div class="code">{
    "operation": "set_permissions",
    "permissions",{"names":[{"user1":3},{"user2":5}]}
    }
    </div>

    <div class="code">{
    "operation": "add_user",
    "user":"johndoe",
    "password":"abc123",
    "encrypted":true
    }
    </div>

    <div class="code">{
    "operation": "delete_user",
    "user":"johndoe"
    }
    </div>
</div>

</body>

</html>
