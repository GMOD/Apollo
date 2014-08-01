define( [
            'dojo/_base/declare',
            'dijit/Menu',
            'dijit/MenuItem', 
            'dijit/MenuSeparator', 
            'dijit/PopupMenuItem',
            'dijit/form/Button',
            'dijit/form/DropDownButton',
            'dijit/DropDownMenu',
            'dijit/form/ComboBox',
            'dijit/form/TextBox',
            'dijit/form/ValidationTextBox',
            'dijit/form/RadioButton',
            'dojox/widget/DialogSimple',
            'dojox/grid/DataGrid',
            'dojox/grid/cells/dijit',
            'dojo/data/ItemFileWriteStore',
            'WebApollo/JSONUtils',
            'WebApollo/BioFeatureUtils',
            'WebApollo/Permission', 
            'WebApollo/SequenceSearch', 
            'WebApollo/EUtils',
            'WebApollo/SequenceOntologyUtils',
            'WebApollo/FormatUtils'
        ],
function(declare, dijitMenu, dijitMenuItem, dijitMenuSeparator , dijitPopupMenuItem, dijitButton,
        dijitDropDownButton, dijitDropDownMenu, dijitComboBox, dijitTextBox, dijitValidationTextBox, 
        dijitRadioButton, dojoxDialogSimple, dojoxDataGrid, dojoxCells, dojoItemFileWriteStore,
        JSONUtils, BioFeatureUtils, Permission, SequenceSearch, EUtils, SequenceOntologyUtils, FormatUtils) {

var context_path = "..";

return declare(null,
{
    constructor: function() {
    
    },

    getSequenceForSelectedFeatures: function(records) {
        var track = this;

        var content = dojo.create("div", { className: "get_sequence" });
        var textArea = dojo.create("textarea", { className: "sequence_area", readonly: true }, content);
        var form = dojo.create("form", { }, content);
        var peptideButtonDiv = dojo.create("div", { className: "first_button_div" }, form);
        var peptideButton = dojo.create("input", { type: "radio", name: "type", checked: true }, peptideButtonDiv);
        var peptideButtonLabel = dojo.create("label", { innerHTML: "Peptide sequence", className: "button_label" }, peptideButtonDiv);
        var cdnaButtonDiv = dojo.create("div", { className: "button_div" }, form);
        var cdnaButton = dojo.create("input", { type: "radio", name: "type" }, cdnaButtonDiv);
        var cdnaButtonLabel = dojo.create("label", { innerHTML: "cDNA sequence", className: "button_label" }, cdnaButtonDiv);
        var cdsButtonDiv = dojo.create("div", { className: "button_div" }, form);
        var cdsButton = dojo.create("input", { type: "radio", name: "type" }, cdsButtonDiv);
        var cdsButtonLabel = dojo.create("label", { innerHTML: "CDS sequence", className: "button_label" }, cdsButtonDiv);
        var genomicButtonDiv = dojo.create("div", { className: "button_div" }, form);
        var genomicButton = dojo.create("input", { type: "radio", name: "type" }, genomicButtonDiv);
        var genomicButtonLabel = dojo.create("label", { innerHTML: "Genomic sequence", className: "button_label" }, genomicButtonDiv);
        var genomicWithFlankButtonDiv = dojo.create("div", { className: "button_div" }, form);
        var genomicWithFlankButton = dojo.create("input", { type: "radio", name: "type" }, genomicWithFlankButtonDiv);
        var genomicWithFlankButtonLabel = dojo.create("label", { innerHTML: "Genomic sequence +/-", className: "button_label" }, genomicWithFlankButtonDiv);
        var genomicWithFlankField = dojo.create("input", { type: "text", size: 5, className: "button_field", value: "500" }, genomicWithFlankButtonDiv);
        var genomicWithFlankFieldLabel = dojo.create("label", { innerHTML: "bases", className: "button_label" }, genomicWithFlankButtonDiv);

        var fetchSequence = function(type) {
            var features = '"features": [';
            for (var i = 0; i < records.length; ++i)  {
                var record = records[i];
                var annot = record.feature;
                var seltrack = record.track;
                var uniqueName = annot.getUniqueName();
                // just checking to ensure that all features in selection are
                // from this track
                if (seltrack === track)  {
                    var trackdiv = track.div;
                    var trackName = track.getUniqueTrackName();

                    if (i > 0) {
                        features += ',';
                    }
                    features += ' { "uniquename": "' + uniqueName + '" } ';
                }
            }
            features += ']';
            var operation = "get_sequence";
            var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"';
                var flank = 0;
                if (type == "genomic_with_flank") {
                        flank = dojo.attr(genomicWithFlankField, "value");
                        postData += ', "flank": ' + flank;
                        type = "genomic";
                }
                postData += ', "type": "' + type + '" }';
                dojo.xhrPost( {
                    postData: postData,
                    url: context_path + "/AnnotationEditorService",
                    handleAs: "json",
                    timeout: 5000 * 1000, // Time in milliseconds
                    load: function(response, ioArgs) {
                        var textAreaContent = "";
                        for (var i = 0; i < response.features.length; ++i) {
                                var feature = response.features[i];
                                var cvterm = feature.type;
                                var residues = feature.residues;
                                var loc = feature.location;
                                textAreaContent += "&gt;" + feature.uniquename + " (" + cvterm.cv.name + ":" + cvterm.name + ") " + residues.length + " residues [" + track.refSeq.name + ":" + (loc.fmin + 1) + "-" + loc.fmax + " " + (loc.strand == -1 ? "-" : loc.strand == 1 ? "+" : "no") + " strand] ["+ type + (flank > 0 ? " +/- " + flank + " bases" : "") + "]\n";
                                var lineLength = 70;
                                for (var j = 0; j < residues.length; j += lineLength) {
                                        textAreaContent += residues.substr(j, lineLength) + "\n";
                                }
                        }
                        dojo.attr(textArea, "innerHTML", textAreaContent);
                    },
                    // The ERROR function will be called in an error case.
                    error: function(response, ioArgs) {
                                track.handleError(response);
                        console.log("Annotation server error--maybe you forgot to login to the server?");
                        console.error("HTTP status code: ", ioArgs.xhr.status);
                        //
                        // dojo.byId("replace").innerHTML = 'Loading the
                        // resource from the server did not work';
                        return response;
                    }

                });
        };
        var callback = function(event) {
            var type;
            var target = event.target || event.srcElement;
            if (target == peptideButton || target == peptideButtonLabel) {
                    dojo.attr(peptideButton, "checked", true);
                    type = "peptide";
            }
            else if (target == cdnaButton || target == cdnaButtonLabel) {
                    dojo.attr(cdnaButton, "checked", true);
                    type = "cdna";
            }
            else if (target == cdsButton || target == cdsButtonLabel) {
                    dojo.attr(cdsButton, "checked", true);
                    type = "cds";
            }
            else if (target == genomicButton || target == genomicButtonLabel) {
                    dojo.attr(genomicButton, "checked", true);
                    type = "genomic";
            }
            else if (target == genomicWithFlankButton || target == genomicWithFlankButtonLabel) {
                    dojo.attr(genomicWithFlankButton, "checked", true);
                    type = "genomic_with_flank";
            }
            fetchSequence(type);
        };

        dojo.connect(peptideButton, "onchange", null, callback);
        dojo.connect(peptideButtonLabel, "onclick", null, callback);
        dojo.connect(cdnaButton, "onchange", null, callback);
        dojo.connect(cdnaButtonLabel, "onclick", null, callback);
        dojo.connect(cdsButton, "onchange", null, callback);
        dojo.connect(cdsButtonLabel, "onclick", null, callback);
        dojo.connect(genomicButton, "onchange", null, callback);
        dojo.connect(genomicButtonLabel, "onclick", null, callback);
        dojo.connect(genomicWithFlankButton, "onchange", null, callback);
        dojo.connect(genomicWithFlankButtonLabel, "onclick", null, callback);

        fetchSequence("peptide");
        this.openDialog("Sequence", content);
    }
});

});


