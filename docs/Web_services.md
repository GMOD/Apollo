# Web Service API


The Apollo Web Service API is a JSON-based REST API to interact with the annotations and other services of Apollo.
Both the request and response JSON objects can contain feature information that are based on the Chado schema.  We use 
the web services API [scripting examples](https://github.com/GMOD/Apollo/blob/master/docs/web_services/examples/)
and we also use them in the Apollo JBrowse plugin.


The most up to date Web Service API documentation is deployed from the source code rest-api-doc annotations. 

See [http://demo.genomearchitect.io/Apollo2/jbrowse/web_services/api](http://demo.genomearchitect.io/Apollo2/jbrowse/web_services/api/) for details

### Warning

If you are sending password you care about over the wire (even if not using web services) it is *highly recommended*
that you use https (which adds encryption ssl) instead of http.


### Examples

We provide an [examples directory](docs/web_services/examples/).

``` 
curl -b cookies.txt -c cookies.txt -e "http://localhost:8080" \
    -H "Content-Type:application/json" \
    -d "{'username': 'demo', 'password': 'demo'}" \
    "http://localhost:8080/apollo/Login?operation=login"
```


Login expects two parameters: <code>username</code> and <code>password</code>, and optionally rememberMe for a
persistent cookie.

A successful login returns a empty JSON object

### Python Client

A [python client](https://github.com/galaxy-genome-annotation/python-apollo) has been provided over many of the
Apollo web services, which is easy to setup:

```
pip install apollo
arrow init # provide Apollo credentials
arrow -h
## have fun
arrow groups get_groups
```

[Documentation on commands](http://python-apollo.readthedocs.io/en/latest/commands.html) and [some examples](http://python-apollo.readthedocs.io/en/latest/arrow.html) working with [jq](https://stedolan.github.io/jq/tutorial/): 


## What is the Web Service API?

For a given Apollo server url (e.g., `https://localhost:8080/apollo` or any other Apollo site on the web), the
Web Service API allows us to make requests to the various "controllers" of the application and perform operations.

The controllers that are available for Apollo include the AnnotationEditorController, the OrganismController, the
IOServiceController for downloads of data, and the UserController for user management.


Most API requests will take:

- The proper url (e.g., to get features from the AnnotationEditorController, we can send requests to
  (e.g `http://localhost/apollo/annotationEditor/getFeatures`)
- username - an authorized user (also uses session if none specified)
- password - password (also uses session if none specified)
- organism - (if applicable) the "common name" of the organism for the operation -- will also pull from the "user
  preferences" if none is specified.
- track/sequence - (if applicable) reference sequence name (shown in sequence panel / genomic
  browse)
- uniquename - (if applicable) the uniquename is a [UUID](https://docs.oracle.com/javase/7/docs/api/java/util/UUID.html)
 used to guarantee a unique ID

### Errors If an error has occurred, a proper HTTP error code (most likely 400 or 500) and an error message.  is
returned, in JSON format:

``` 
{ "error": "error message" }
```


### Cookies

The Apollo Login creates a JSESSIONID cookie and rememberMe cookie (if applicable) and these can be used in
downstream API requests (for example, by setting -b cookies.txt in curl will preserve the cookie in the request).

You can also pass username/password to individual API requests and these will authenticate each individual request. 



### Representing features in JSON

Most requests and responses will contain an array of `feature` JSON objects named `features`.  The `feature` object is
based on the Chado `feature`, `featureloc`, `cv`, and `cvterm` tables.

``` 
{
    "residues": "$residues",
    "type": {
        "cv": {
            "name": "$cv_name"
        },
        "name": "$cv_term"
    },
    "location": {
        "fmax": $rightmost_intrabase_coordinate_of_feature,
        "fmin": $leftmost_intrabase_coordinate_of_feature,
        "strand": $strand
    },
    "uniquename": "$feature_unique_name"
    "children": [$array_of_child_features]
    "properties": [$array_of_properties]
}
```
where:

* `residues` - A sequence of alphabetic characters representing biological residues (nucleic acids, amino acids)
 [string]
* `type.cv.name` - The name of the ontology [string] `type.name` - The name for the cvterm [string]
* `location.fmax` - The rightmost/maximal intrabase boundary in the linear range [integer]
* `location.fmin` - The leftmost/minimal intrabase boundary in the linear range [integer]
*  `strand` - The orientation/directionality of the location. Should be 0, -1 or +1 [integer]
* `uniquename` - The unique name for a feature [string]
* `children` - Array of child feature objects [array]
* `properties` - Array of properties (including frameshifts for transcripts) [array]

Note that different operations will require different fields to be set (which will be elaborated upon in each operation
section).


## Web Services API

The most up to date Web Service API documentation is deployed from the source code rest-api-doc annotations


See [http://demo.genomearchitect.io/Apollo2/jbrowse/web_services/api](http://demo.genomearchitect.io/Apollo2/jbrowse/web_services/api) for details
