define([ 'dojo/_base/declare',
         'dojo/_base/array',
         'JBrowse/Util',
         'JBrowse/Model/SimpleFeature', 
         'WebApollo/SequenceOntologyUtils'
       ],
       function( declare, array, Util, SimpleFeature, SeqOnto ) {

function JSONUtils() {
}

JSONUtils.verbose_conversion = false;

/**
*  creates a feature in JBrowse JSON format
*  takes as arguments:
*      afeature: feature in ApolloEditorService JSON format,
*      arep: ArrayRepr for kind of JBrowse feature to output
*      OLD: fields: array specifying order of fields for JBrowse feature
*      OLD: subfields:  array specifying order of fields for subfeatures of JBrowse feature
*   "CDS" type feature in Apollo JSON format is from genomic start of translation to genomic end of translation,
*          (+ stop codon), regardless of intons, so one per transcript (usually)
*   "CDS" type feature in JBrowse JSON format is a CDS _segment_, which are piecewise and broken up by introns
*          therefore commonyly have multiple CDS segments
*
*/
// JSONUtils.createJBrowseFeature = function(afeature, fields, subfields)  {
var JAFeature = declare( SimpleFeature, {
    "-chains-": {
        constructor: "manual"
    },
    constructor: function( afeature, parent ) {
        this.afeature = afeature;
        if (parent)  { this._parent = parent; }
        
        // get the main data
        var loc = afeature.location;
        var pfeat = this;
        this.data = {
            start: loc.fmin,
            end: loc.fmax,
            strand: loc.strand,
            name: afeature.name,
            parent_id: afeature.parent_id,
            parent_type: afeature.parent_type ? afeature.parent_type.name : undefined,
            type: afeature.type.name, 
            properties: afeature.properties
        };

        if (this.data.type === "CDS")  { 
            this.data.type = "wholeCDS"; 
        }
        else if (this.data.type === "stop_codon_read_through") {
            parent.data.readThroughStopCodon = true;
        }
    
        this._uniqueID = afeature.uniquename;

        // this doesn't work, since can be multiple properties with same CV term (comments, for example)
        //   could create arrray for each flattened cv-name for multiple values, but not sure what the point would be over 
        //   just making sure can access via get('properties') via above assignment into data object
        // parse the props
/*      var props = afeature.properties;
        dojo.forEach( props, function( p ) {
            var pn = p.type.cv.name+':'+p.type.name;
            this.data[pn] = p.value;
        }, this);
*/

        if (afeature.properties) {
            for (var i = 0; i < afeature.properties.length; ++i) {
                var property = afeature.properties[i];
                if (property.type.name == "comment" && property.value == "Manually set translation start") {
                    // jfeature.manuallySetTranslationStart = true;
                    this.data.manuallySetTranslationStart = true;   // so can call feat.get('manuallySetTranslationStart')
                    if (this.parent())  { parent.data.manuallySetTranslationStart = true; }
                }
                else if (property.type.name == "comment" && property.value == "Manually set translation end") {
                    this.data.manuallySetTranslationEnd = true;   // so can call feat.get('manuallySetTranslationEnd')
                    if (this.parent())  { parent.data.manuallySetTranslationEnd = true; }
                }
                else if (property.type.name == "owner") {
                    this.data.owner = property.value;
                }
                else if (property.type.name == "feature_property") {
                    if (property.value == "locked=true") {
                        this.data.locked = true;
                    }
                }
            }
        }
        
        if (!parent) {
            if (afeature.children) {
                var descendants = [];
                for (var i = 0; i < afeature.children.length; ++i) {
                    var child = afeature.children[i];
                    if (child.children) {
                        for (var j = 0; j < child.children.length; ++j) {
                            JSONUtils.flattenFeature(child.children[j], descendants);
                        }
                    }
                }
                afeature.children = afeature.children.concat(descendants);
            }
            else {
                var child = dojo.clone(afeature);
                child.uniquename += "-clone";
                this.set("cloned_subfeatures", true);
                afeature.children = [ child ];
            }
        }
        
        // moved subfeature assignment to bottom of feature construction, since subfeatures may need to call method on their parent
        //     only thing subfeature constructor won't have access to is parent.data.subfeatures
        // get the subfeatures              
        this.data.subfeatures = array.map( afeature.children, function(s) {
            return new JAFeature( s, pfeat);
        } );

    },
    
    getUniqueName: function() {
        if (this.parent() && this.parent().get("cloned_subfeatures")) {
            return this.parent().id();
        }
        return this.id();
    }
});

JSONUtils.JAFeature = JAFeature;

JSONUtils.createJBrowseFeature = function( afeature )  {
    return new JAFeature( afeature );
};

JSONUtils.numberWithCommas = function( x)  {
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
};

JSONUtils.flattenFeature = function(feature, descendants) {
    descendants.push(feature);
    if (feature.children) {
        for (var i = 0; i < feature.children.length; ++i) {
            JSONUtils.flattenFeature(feature.children[i], descendants);
        }
        feature.children = [];
    }
};


/**
 *  takes any JBrowse feature, returns a SimpleFeature "copy", 
 *        for which all properties returned by tags() are mutable (has set() method)
 *  needed since JBrowse features no longer necessarily mutable
 *    feature requirements:
 *         functions: id, parent, tags, get
 *         if subfeatures, then returned as array by feature.get('subfeatures')
 *      
 */
JSONUtils.makeSimpleFeature = function(feature, parent)  {
    var result = new SimpleFeature({id: feature.id(), parent: (parent ? parent : feature.parent()) });
    var ftags = feature.tags();
    for (var tindex = 0; tindex < ftags.length; tindex++)  {  
        var tag = ftags[tindex];
        // forcing lower case, since still having case issues with NCList features
        result.set(tag.toLowerCase(), feature.get(tag.toLowerCase()));
    }
    var subfeats = feature.get('subfeatures');
    if (subfeats && (subfeats.length > 0))  {
        var simple_subfeats = [];
        for (var sindex = 0; sindex < subfeats.length; sindex++)  {
            var simple_subfeat = JSONUtils.makeSimpleFeature(subfeats[sindex], result);
            simple_subfeats.push(simple_subfeat);
        }
        result.set('subfeatures', simple_subfeats);
    }
    return result;
};

/**
*  creates a sequence alteration in JBrowse JSON format
*  takes as arguments:
*      arep: ArrayRepr for kind of JBrowse feature to output
*      afeature: sequence alteration in ApolloEditorService JSON format,
*/
JSONUtils.createJBrowseSequenceAlteration = function( afeature )  {
    var loc = afeature.location; 
    var uid = afeature.uniquename;
    var justification;
    for (var i = 0; i < afeature.properties.length; i++) {
        if (afeature.properties[i].type.name === "justification") {
            justification = afeature.properties[i].value;
        }
    }

    return new SimpleFeature({
        data: {
            start:    loc.fmin,
            end:      loc.fmax,
            strand:   loc.strand,
            id:       uid,
            type:     afeature.type.name,
            residues: afeature.residues,
            seq:      afeature.residues,
            justification: justification
        },
        id: uid
    });
};


/** 
*  creates a feature in ApolloEditorService JSON format
*  takes as argument:
*       jfeature: a feature in JBrowse JSON format, 
*       fields: array specifying order of fields in jfeature
*       subfields: array specifying order of fields in subfeatures of jfeature
*       specified_type (optional): type passed in that overrides type info for jfeature
*  ApolloEditorService format:
*    { 
*       "location" : { "fmin": fmin, "fmax": fmax, "strand": strand }, 
*       "type": { "cv": { "name":, cv },   // typical cv name: "SO" (Sequence Ontology)
*                 "name": cvterm },        // typical name: "transcript"
*       "children": { __recursive ApolloEditorService feature__ }
*    }
* 
*   For ApolloEditorService "add_feature" call to work, need to have "gene" as toplevel feature, 
*         then "transcript", then ???
*                 
*    JBrowse JSON fields example: ["start", "end", "strand", "id", "subfeatures"]
*
*    type handling
*    if specified_type arg present, it determines type name
*    else if fields has a "type" field, use that to determine type name
*    else don't include type 
*
*    ignoring JBrowse ID / name fields for now
*    currently, for features with lazy-loaded children, ignores children 
*/
JSONUtils.createApolloFeature = function( jfeature, specified_type, useName, specified_subtype )   {

    var diagnose =  (JSONUtils.verbose_conversion && jfeature.children() && jfeature.children().length > 0);
    if (diagnose)  { 
        console.log("converting JBrowse feature to Apollo feture, specified type: " + specified_type); 
        console.log(jfeature);
    }

    var afeature = new Object();
    var astrand;
    // Apollo feature strand must be an integer
    //     either 1 (plus strand), -1 (minus strand), or 0? (not stranded or strand is unknown?)
    switch (jfeature.get('strand')) {  // strand
    case 1:
    case '+':
        astrand = 1; break;
    case -1:
    case '-':
        astrand = -1; break;
    default:
        astrand = 0; // either not stranded or strand is uknown
    }
    
    afeature.location = {
        "fmin": jfeature.get('start'),
        "fmax": jfeature.get('end'),
        "strand": astrand
    };

    var typename;
    if (specified_type)  {
        typename = specified_type;
    }
    else if ( jfeature.get('type') ) {
        typename = jfeature.get('type');
    }

    if (typename)  {
        afeature.type = {
            "cv": {
            "name": "sequence"
        }
    };
    afeature.type.name = typename;
    }

    var name = jfeature.get('name');
    if (useName && name) {
        afeature.name = name;
    }
    
    /*
    afeature.properties = [];
    var property = { value : "source_id=" + jfeature.get('id'),
            type : {
                    cv: {
                        name: "feature_property"
                    },
                    name: "feature_property"
            }
    };
    afeature.properties.push(property);
    */

    if (diagnose) { console.log("converting to Apollo feature: " + typename); }
    var subfeats;
    // use filteredsubs if present instead of subfeats?
    //    if (jfeature.filteredsubs)  { subfeats = jfeature.filteredsubs; }
    //    else  { subfeats = jfeature.get('subfeatures'); }
    subfeats = jfeature.get('subfeatures'); 
    if( subfeats && subfeats.length )  {
        afeature.children = [];
        var slength = subfeats.length;
        var cds;
        var cdsFeatures = [];
        var foundExons = false;
        
        var updateCds = function(subfeat) {
            if (!cds) {
                cds = new SimpleFeature({id: "cds", parent: jfeature});
                cds.set('start', subfeat.get('start'));
                cds.set('end', subfeat.get('end'));
                cds.set('strand', subfeat.get('strand'));
                cds.set('type', 'CDS');
            }
            else {
                if (subfeat.get("start") < cds.get("start")) {
                    cds.set("start", subfeat.get("start"));
                }
                if (subfeat.get("end") > cds.get("end")) {
                    cds.set("end", subfeat.get("end"));
                }
            }
        };
        
        for (var i=0; i<slength; i++)  {
            var subfeat = subfeats[i];
            var subtype = subfeat.get('type');
                var converted_subtype = specified_subtype || subtype;
                if (!specified_subtype) {
                    if (SeqOnto.exonTerms[subtype])  {
                        // definitely an exon, leave exact subtype as is 
                        // converted_subtype = "exon"
                    }
                    else if (subtype === "wholeCDS" || subtype === "polypeptide") {
                        // normalize to "CDS" sequnce ontology term
                        // converted_subtype = "CDS";
                        updateCds(subfeat);
                        converted_subtype = null;
                    }
                    else if (SeqOnto.cdsTerms[subtype])  {
                        // other sequence ontology CDS terms, leave unchanged
                        updateCds(subfeat);
                        converted_subtype = null;
                        cdsFeatures.push(subfeat);
                    }
                    else if (SeqOnto.spliceTerms[subtype])  {  
                        // splice sites -- filter out?  leave unchanged?
                        // 12/16/2012 filtering out for now, causes errors in AnnotTrack duplication operation
                        converted_subtype = null;  // filter out
                    }
                    else if (SeqOnto.startCodonTerms[subtype] || SeqOnto.stopCodonTerms[subtype])  {
                        // start and stop codons -- filter out?  leave unchanged?
                        // 12/16/2012 filtering out for now, causes errors in AnnotTrack createAnnotation operation
                        converted_subtype = null;  // filter out
                    }
                    else if (SeqOnto.intronTerms[subtype])  {
                        // introns -- filter out?  leave unchanged?
                        converted_subtype = null;  // filter out
                    }
                    /*
                    else if (SeqOnto.utrTerms[subtype]) {
                        // filter out UTR
                        converted_subtype = null;
                    }
                    */
                    else  { 
                        // convert everything else to exon???
                        // need to do this since server only creates exons for "exon" and descendant terms
                        converted_subtype = "exon";
                    }
                }
                if (SeqOnto.exonTerms[subtype]) {
                    foundExons = true;
                }
                if (converted_subtype)  {
                afeature.children.push( JSONUtils.createApolloFeature( subfeat, converted_subtype ) );
                    if (diagnose)  { console.log("    subfeat original type: " + subtype + ", converted type: " + converted_subtype); }
                }
                else {
                    if (diagnose)  { console.log("    edited out subfeature, type: " + subtype); }
                }
        }
        if (cds) {
            afeature.children.push( JSONUtils.createApolloFeature( cds, "CDS"));
            if (!foundExons) {
                for (var i = 0; i < cdsFeatures.length; ++i) {
                    afeature.children.push(JSONUtils.createApolloFeature(cdsFeatures[i], "exon"));
                }
            }
        }
    }
    else if ( specified_type === 'transcript' )  {
        // special casing for Apollo "transcript" features being created from 
        //    JBrowse top-level features that have no children
        // need to create an artificial exon child the same size as the transcript
        var fake_exon = new SimpleFeature({id: jfeature.id()+"_dummy_exon", parent: jfeature});
        fake_exon.set('start', jfeature.get('start'));
        fake_exon.set('end', jfeature.get('end'));
        fake_exon.set('strand', jfeature.get('strand'));
        fake_exon.set('type', 'exon');
        afeature.children = [ JSONUtils.createApolloFeature( fake_exon ) ];
    }
    if (diagnose)  { console.log("result:"); console.log(afeature); }
    return afeature;
};

// experimenting with forcing export of JSONUtils into global namespace...
window.JSONUtils = JSONUtils;

return JSONUtils;
 
});