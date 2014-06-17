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

   
    createAnnotationInfoEditorPanelForFeatureSideBar: function(uniqueName, trackName, selector, reload) {
        console.log("createAnnotationInfoEditorPanelForFeatureSideBar");
        var track = this;
//      var hasWritePermission = this.hasWritePermission();
        var hasWritePermission = this.canEdit(this.store.getFeatureById(uniqueName));
        var content = dojo.create("span");
        var header = dojo.create("div", { className: "annotation_sidebar_header" }, content);

        var nameDiv = dojo.create("div", { class: "annotation_sidebar_field_section" }, content);
        var nameLabel = dojo.create("label", { innerHTML: "Name", class: "annotation_sidebar_label" }, nameDiv);
        var nameField = new dijitTextBox({ class: "annotation_sidebar_field"});
        dojo.place(nameField.domNode, nameDiv);
        
        if (!hasWritePermission) {
            nameField.set("disabled", true);
        }
        var timeout = 100;
        
        var escapeString = function(str) {
            return str.replace(/(["'])/g, "\\$1");
        };
        
        function init() {
            var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
            var operation = "get_annotation_info_editor_data";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            dojo.xhrPost( {
                sync: true,
                postData: postData,
                url: context_path + "/AnnotationEditorService",
                handleAs: "json",
                timeout: 5000 * 1000, // Time in milliseconds
                load: function(response, ioArgs) {
                    var feature = response.features[0];
                    var config = track.annotationInfoEditorConfigs[feature.type.cv.name + ":" + feature.type.name] || track.annotationInfoEditorConfigs["default"];
                    initName(feature);
                }
            });
        };
        var initName = function(feature) {
            if (feature.name) {
                nameField.set("value", feature.name);
            }
            var oldName;
            dojo.connect(nameField, "onFocus", function() {
                oldName = nameField.get("value");
            });
            dojo.connect(nameField, "onBlur", function() {
                var newName = nameField.get("value");
                if (oldName != newName) {
                    updateName(newName);
                    if (selector) {
                        var select = selector.store.get(feature.uniquename).then(function(select) {
                            selector.store.setValue(select, "label", newName);
                        });
                    }
                }
            });
        };
        init();
        return content;
    },
    createAnnotationInfoEditorPanelForFeature: function(uniqueName, trackName, selector, reload) {
        var track = this;
//      var hasWritePermission = this.hasWritePermission();
        var hasWritePermission = this.canEdit(this.store.getFeatureById(uniqueName));
        var content = dojo.create("span");
        
        var header = dojo.create("div", { className: "annotation_info_editor_header" }, content);

        var nameDiv = dojo.create("div", { class: "annotation_info_editor_field_section" }, content);
        var nameLabel = dojo.create("label", { innerHTML: "Name", class: "annotation_info_editor_label" }, nameDiv);
        var nameField = new dijitTextBox({ class: "annotation_editor_field"});
        dojo.place(nameField.domNode, nameDiv);
         // var nameField = new dojo.create("input", { type: "text" }, nameDiv);

        var symbolDiv = dojo.create("div", { class: "annotation_info_editor_field_section" }, content);
        var symbolLabel = dojo.create("label", { innerHTML: "Symbol", class: "annotation_info_editor_label" }, symbolDiv);
        var symbolField = new dijitTextBox({ class: "annotation_editor_field"});
        dojo.place(symbolField.domNode, symbolDiv);
        
        var descriptionDiv = dojo.create("div", { class: "annotation_info_editor_field_section" }, content);
        var descriptionLabel = dojo.create("label", { innerHTML: "Description", class: "annotation_info_editor_label" }, descriptionDiv);
        var descriptionField = new dijitTextBox({ class: "annotation_editor_field"});
        dojo.place(descriptionField.domNode, descriptionDiv);

        var dateCreationDiv = dojo.create("div", { class: "annotation_info_editor_field_section" }, content);
        var dateCreationLabel = dojo.create("label", { innerHTML: "Created", class: "annotation_info_editor_label" }, dateCreationDiv);
        var dateCreationField = new dijitTextBox({ class: "annotation_editor_field", readonly: true });
        dojo.place(dateCreationField.domNode, dateCreationDiv);

        var dateLastModifiedDiv = dojo.create("div", { class: "annotation_info_editor_field_section" }, content);
        var dateLastModifiedLabel = dojo.create("label", { innerHTML: "Last modified", class: "annotation_info_editor_label" }, dateLastModifiedDiv);
        var dateLastModifiedField = new dijitTextBox({ class: "annotation_editor_field", readonly: true });
        dojo.place(dateLastModifiedField.domNode, dateLastModifiedDiv);
        
        var statusDiv = dojo.create("div", { class: "annotation_info_editor_section" }, content);
        var statusLabel = dojo.create("div", { class: "annotation_info_editor_section_header", innerHTML: "Status" }, statusDiv);
        var statusFlags = dojo.create("div", { class: "status" }, statusDiv);
        var statusRadios = new Object();

        var dbxrefsDiv = dojo.create("div", { class: "annotation_info_editor_section" }, content);
        var dbxrefsLabel = dojo.create("div", { class: "annotation_info_editor_section_header", innerHTML: "DBXRefs" }, dbxrefsDiv);
        var dbxrefsTable = dojo.create("div", { class: "dbxrefs", id: "dbxrefs_" + (selector ? "child" : "parent") }, dbxrefsDiv);
        var dbxrefButtonsContainer = dojo.create("div", { style: "text-align: center;" }, dbxrefsDiv);
        var dbxrefButtons = dojo.create("div", { class: "annotation_info_editor_button_group" }, dbxrefButtonsContainer);
        var addDbxrefButton = dojo.create("button", { innerHTML: "Add", class: "annotation_info_editor_button" }, dbxrefButtons);
        var deleteDbxrefButton = dojo.create("button", { innerHTML: "Delete", class: "annotation_info_editor_button" }, dbxrefButtons);

        var attributesDiv = dojo.create("div", { class: "annotation_info_editor_section" }, content);
        var attributesLabel = dojo.create("div", { class: "annotation_info_editor_section_header", innerHTML: "Attributes" }, attributesDiv);
        var attributesTable = dojo.create("div", { class: "attributes", id: "attributes_" + (selector ? "child" : "parent")  }, attributesDiv);
        var attributeButtonsContainer = dojo.create("div", { style: "text-align: center;" }, attributesDiv);
        var attributeButtons = dojo.create("div", { class: "annotation_info_editor_button_group" }, attributeButtonsContainer);
        var addAttributeButton = dojo.create("button", { innerHTML: "Add", class: "annotation_info_editor_button" }, attributeButtons);
        var deleteAttributeButton = dojo.create("button", { innerHTML: "Delete", class: "annotation_info_editor_button" }, attributeButtons);

        var pubmedIdsDiv = dojo.create("div", { class: "annotation_info_editor_section" }, content);
        var pubmedIdsLabel = dojo.create("div", { class: "annotation_info_editor_section_header", innerHTML: "Pubmed IDs" }, pubmedIdsDiv);
        var pubmedIdsTable = dojo.create("div", { class: "pubmed_ids", id: "pubmd_ids_" + (selector ? "child" : "parent")  }, pubmedIdsDiv);
        var pubmedIdButtonsContainer = dojo.create("div", { style: "text-align: center;" }, pubmedIdsDiv);
        var pubmedIdButtons = dojo.create("div", { class: "annotation_info_editor_button_group" }, pubmedIdButtonsContainer);
        var addPubmedIdButton = dojo.create("button", { innerHTML: "Add", class: "annotation_info_editor_button" }, pubmedIdButtons);
        var deletePubmedIdButton = dojo.create("button", { innerHTML: "Delete", class: "annotation_info_editor_button" }, pubmedIdButtons);
        
        var goIdsDiv = dojo.create("div", { class: "annotation_info_editor_section" }, content);
        var goIdsLabel = dojo.create("div", { class: "annotation_info_editor_section_header", innerHTML: "Gene Ontology IDs" }, goIdsDiv);
        var goIdsTable = dojo.create("div", { class: "go_ids", id: "go_ids_" + (selector ? "child" : "parent")  }, goIdsDiv);
        var goIdButtonsContainer = dojo.create("div", { style: "text-align: center;" }, goIdsDiv);
        var goIdButtons = dojo.create("div", { class: "annotation_info_editor_button_group" }, goIdButtonsContainer);
        var addGoIdButton = dojo.create("button", { innerHTML: "Add", class: "annotation_info_editor_button" }, goIdButtons);
        var deleteGoIdButton = dojo.create("button", { innerHTML: "Delete", class: "annotation_info_editor_button" }, goIdButtons);

        var commentsDiv = dojo.create("div", { class: "annotation_info_editor_section" }, content);
        var commentsLabel = dojo.create("div", { class: "annotation_info_editor_section_header", innerHTML: "Comments" }, commentsDiv);
        var commentsTable = dojo.create("div", { class: "comments", id: "comments_" + (selector ? "child" : "parent")  }, commentsDiv);
        var commentButtonsContainer = dojo.create("div", { style: "text-align: center;" }, commentsDiv);
        var commentButtons = dojo.create("div", { class: "annotation_info_editor_button_group" }, commentButtonsContainer);
        var addCommentButton = dojo.create("button", { innerHTML: "Add", class: "annotation_info_editor_button" }, commentButtons);
        var deleteCommentButton = dojo.create("button", { innerHTML: "Delete", class: "annotation_info_editor_button" }, commentButtons);
        
        if (!hasWritePermission) {
            nameField.set("disabled", true);
            symbolField.set("disabled", true);
            descriptionField.set("disabled", true);
            dateCreationField.set("disabled", true);
            dateLastModifiedField.set("disabled", true);
            dojo.attr(addDbxrefButton, "disabled", true);
            dojo.attr(deleteDbxrefButton, "disabled", true);
            dojo.attr(addAttributeButton, "disabled", true);
            dojo.attr(deleteAttributeButton, "disabled", true);
            dojo.attr(addPubmedIdButton, "disabled", true);
            dojo.attr(deletePubmedIdButton, "disabled", true);
            dojo.attr(addGoIdButton, "disabled", true);
            dojo.attr(deleteGoIdButton, "disabled", true);
            dojo.attr(addCommentButton, "disabled", true);
            dojo.attr(deleteCommentButton, "disabled", true);
        }
        
        var pubmedIdDb = "PMID";
        var goIdDb = "GO";
        
        var timeout = 100;
        
        var escapeString = function(str) {
            return str.replace(/(["'])/g, "\\$1");
        };
        
        function init() {
            var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
            var operation = "get_annotation_info_editor_data";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            dojo.xhrPost( {
                sync: true,
                postData: postData,
                url: context_path + "/AnnotationEditorService",
                handleAs: "json",
                timeout: 5000 * 1000, // Time in milliseconds
                load: function(response, ioArgs) {
                    var feature = response.features[0];
                    var config = track.annotationInfoEditorConfigs[feature.type.cv.name + ":" + feature.type.name] || track.annotationInfoEditorConfigs["default"];
                    initType(feature);
                    initName(feature);
                    initSymbol(feature);
                    initDescription(feature);
                    initDates(feature);
                    initStatus(feature, config);
                    initDbxrefs(feature, config);
                    initAttributes(feature, config);
                    initPubmedIds(feature, config);
                    initGoIds(feature, config);
                    initComments(feature, config);
                }
            });
        };
        
        function initTable(domNode, tableNode, table, timeout) {
            var id = dojo.attr(tableNode, "id");
            var node = dojo.byId(id);
            if (!node) {
                setTimeout(function() {
                    initTable(domNode, tableNode, table, timeout);
                    return;
                }, timeout);
                return;
            }
            dojo.place(domNode, tableNode, "first");
            table.startup();
        }
        
        var initType = function(feature) {
            header.innerHTML = feature.type.name;
        };
        
        var initName = function(feature) {
            if (feature.name) {
                nameField.set("value", feature.name);
            }
            var oldName;
            dojo.connect(nameField, "onFocus", function() {
                oldName = nameField.get("value");
            });
            dojo.connect(nameField, "onBlur", function() {
                var newName = nameField.get("value");
                if (oldName != newName) {
                    updateName(newName);
                    if (selector) {
                        var select = selector.store.get(feature.uniquename).then(function(select) {
                            selector.store.setValue(select, "label", newName);
                        });
                    }
                }
            });
        };
        
        var initSymbol = function(feature) {
            if (feature.symbol) {
                symbolField.set("value", feature.symbol);
            }
            var oldSymbol;
            dojo.connect(symbolField, "onFocus", function() {
                oldSymbol = symbolField.get("value");
            });
            dojo.connect(symbolField, "onBlur", function() {
                var newSymbol = symbolField.get("value");
                if (oldSymbol != newSymbol) {
                    updateSymbol(newSymbol);
                }
            });
        };
        
        var initDescription = function(feature) {
            if (feature.description) {
                descriptionField.set("value", feature.description);
            }
            var oldDescription;
            dojo.connect(descriptionField, "onFocus", function() {
                oldDescription = descriptionField.get("value");
            });
            dojo.connect(descriptionField, "onBlur", function() {
                var newDescription = descriptionField.get("value");
                if (oldDescription != newDescription) {
                    updateDescription(newDescription);
                }
            });
        };
        
        var initDates = function(feature) {
            if (feature.date_creation) {
                dateCreationField.set("value", FormatUtils.formatDate(feature.date_creation));
            }
            if (feature.date_last_modified) {
                dateLastModifiedField.set("value", FormatUtils.formatDate(feature.date_last_modified));
            }
        };
        
        var initStatus = function(feature, config) {
            var maxLength = 0;
            var status = config.status;
            if (status) {
                for (var i = 0; i < status.length; ++i) {
                    if (status[i].length > maxLength) {
                        maxLength = status[i].length;
                    }
                }
                for (var i = 0; i < status.length; ++i) {
                    var statusRadioDiv = dojo.create("span", { class: "annotation_info_editor_radio", style: "width:" + (maxLength * 0.75) + "em;" }, statusFlags);
                    var statusRadio = new dijitRadioButton({ value: status[i], name: "status_" + uniqueName, checked: status[i] == feature.status ? true : false });
                    if (!hasWritePermission) {
                        statusRadio.set("disabled", true);
                    }
                    dojo.place(statusRadio.domNode, statusRadioDiv);
                    var statusLabel = dojo.create("label", { innerHTML: status[i], class: "annotation_info_editor_radio_label" }, statusRadioDiv);
                    statusRadios[status[i]] = statusRadio;
                    dojo.connect(statusRadio, "onMouseDown", function(div, radio, label) {
                        return function(event) {
                            if (radio.checked) {
                                deleteStatus();
                                dojo.place(new dijitRadioButton({ value: status[i], name: "status_" + uniqueName, checked: false }).domNode, radio.domNode, "replace");
                            }
                        };
                    }(statusRadioDiv, statusRadio, statusLabel));
                    dojo.connect(statusRadio, "onChange", function(label) {
                        return function(selected) {
                            if (selected && hasWritePermission) {
                                updateStatus(label);
                            }
                        };
                    }(status[i]));
                }
            }
            else {
                dojo.style(statusDiv, "display", "none");
            }
        };
        
        var initDbxrefs = function(feature, config) {
            if (config.hasDbxrefs) {
                var oldDb;
                var oldAccession;
                var dbxrefs = new dojoItemFileWriteStore({
                    data: {
                        items: []
                    }
                });
                for (var i = 0; i < feature.dbxrefs.length; ++ i) {
                    var dbxref = feature.dbxrefs[i];
                    if (dbxref.db != pubmedIdDb && dbxref.db != goIdDb) {
                        dbxrefs.newItem({ db: dbxref.db, accession: dbxref.accession });
                    }
                }
                var dbxrefTableLayout = [{
                    cells: [
                            {
                                name: 'DB',
                                field: 'db',
                                width: '40%',
                                formatter: function(db) {
                                    if (!db) {
                                        return "Enter new DB";
                                    }
                                    return db;
                                },
                                editable: hasWritePermission
                            },
                            {
                                name: 'Accession',
                                field: 'accession',
                                width: '60%',
                                formatter: function(accession) {
                                    if (!accession) {
                                        return "Enter new accession";
                                    }
                                    return accession;
                                },
                                editable: hasWritePermission
                            }
                           ]
                }];

                var dbxrefTable = new dojoxDataGrid({
                    singleClickEdit: true,
                    store: dbxrefs,
                    updateDelay: 0,
                    structure: dbxrefTableLayout
                });
                
                var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function() {
                    initTable(dbxrefTable.domNode, dbxrefsTable, dbxrefTable);
                    dojo.disconnect(handle);
                });
                if (reload) {
                    initTable(dbxrefTable.domNode, dbxrefsTable, dbxrefTable, timeout);
                }
                
                
                var dirty = false;
                dojo.connect(dbxrefTable, "onStartEdit", function(inCell, inRowIndex) {
                    if (!dirty) {
                        oldDb = dbxrefTable.store.getValue(dbxrefTable.getItem(inRowIndex), "db");
                        oldAccession = dbxrefTable.store.getValue(dbxrefTable.getItem(inRowIndex), "accession");
                        dirty = true;
                    }
                });
                
                dojo.connect(dbxrefTable, "onCancelEdit", function(inRowIndex) {
                    dbxrefTable.store.setValue(dbxrefTable.getItem(inRowIndex), "db", oldDb);
                    dbxrefTable.store.setValue(dbxrefTable.getItem(inRowIndex), "accession", oldAccession);
                    dirty = false;
                });
                
                dojo.connect(dbxrefTable, "onApplyEdit", function(inRowIndex) {
                    var newDb = dbxrefTable.store.getValue(dbxrefTable.getItem(inRowIndex), "db");
                    var newAccession = dbxrefTable.store.getValue(dbxrefTable.getItem(inRowIndex), "accession");
                    if (!newDb || !newAccession) {
                    }
                    else if (!oldDb || !oldAccession) {
                        addDbxref(newDb, newAccession);
                    }
                    else {
                        if (newDb != oldDb || newAccession != oldAccession) {
                            updateDbxref(oldDb, oldAccession, newDb, newAccession);
                        }
                    }
                    dirty = false;
                });
                
                dojo.connect(addDbxrefButton, "onclick", function() {
                    dbxrefTable.store.newItem({ db: "", accession: "" });
                    dbxrefTable.scrollToRow(dbxrefTable.rowCount);
                });
                
                dojo.connect(deleteDbxrefButton, "onclick", function() {
                    var toBeDeleted = new Array();
                    var selected = dbxrefTable.selection.getSelected();
                    for (var i = 0; i < selected.length; ++i) {
                        var item = selected[i];
                        var db = dbxrefTable.store.getValue(item, "db");
                        var accession = dbxrefTable.store.getValue(item, "accession");
                        toBeDeleted.push({ db: db, accession: accession });
                    }
                    dbxrefTable.removeSelectedRows();
                    deleteDbxrefs(toBeDeleted);
                });
            }
            else {
                dojo.style(dbxrefsDiv, "display", "none");
            }
        };
        
        var initAttributes = function(feature, config) {
            if (config.hasAttributes) {
                var oldTag;
                var oldValue;
                var attributes = new dojoItemFileWriteStore({
                    data: {
                        items: []
                    }
                });
                for (var i = 0; i < feature.non_reserved_properties.length; ++ i) {
                    var attribute = feature.non_reserved_properties[i];
                    attributes.newItem({ tag: attribute.tag, value: attribute.value });
                }
                var attributeTableLayout = [{
                    cells: [
                            {
                                name: 'Tag',
                                field: 'tag',
                                width: '40%',
                                formatter: function(tag) {
                                    if (!tag) {
                                        return "Enter new tag";
                                    }
                                    return tag;
                                },
                                editable: hasWritePermission
                            },
                            {
                                name: 'Value',
                                field: 'value',
                                width: '60%',
                                formatter: function(value) {
                                    if (!value) {
                                        return "Enter new value";
                                    }
                                    return value;
                                },
                                editable: hasWritePermission
                            }
                           ]
                }];

                var attributeTable = new dojoxDataGrid({
                    singleClickEdit: true,
                    store: attributes,
                    updateDelay: 0,
                    structure: attributeTableLayout
                });
                
                var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function() {
                    initTable(attributeTable.domNode, attributesTable, attributeTable);
                    dojo.disconnect(handle);
                });
                if (reload) {
                    initTable(attributeTable.domNode, attributesTable, attributeTable, timeout);
                }
                
                var dirty = false;

                dojo.connect(attributeTable, "onStartEdit", function(inCell, inRowIndex) {
                    if (!dirty) {
                        oldTag = attributeTable.store.getValue(attributeTable.getItem(inRowIndex), "tag");
                        oldValue = attributeTable.store.getValue(attributeTable.getItem(inRowIndex), "value");
                        dirty = true;
                    }
                });
                
                dojo.connect(attributeTable, "onCancelEdit", function(inRowIndex) {
                    attributeTable.store.setValue(attributeTable.getItem(inRowIndex), "tag", oldTag);
                    attributeTable.store.setValue(attributeTable.getItem(inRowIndex), "value", oldValue);
                    dirty = false;
                });
                
                dojo.connect(attributeTable, "onApplyEdit", function(inRowIndex) {
                    var newTag = attributeTable.store.getValue(attributeTable.getItem(inRowIndex), "tag");
                    var newValue = attributeTable.store.getValue(attributeTable.getItem(inRowIndex), "value");
                    if (!newTag || !newValue) {
                    }
                    else if (!oldTag || !oldValue) {
                        addAttribute(newTag, newValue);
                    }
                    else {
                        if (newTag != oldTag || newValue != oldValue) {
                            updateAttribute(oldTag, oldValue, newTag, newValue);
                        }
                    }
                    dirty = false;
                });
                
                dojo.connect(addAttributeButton, "onclick", function() {
                    attributeTable.store.newItem({ tag: "", value: "" });
                    attributeTable.scrollToRow(attributeTable.rowCount);
                });
                
                dojo.connect(deleteAttributeButton, "onclick", function() {
                    var toBeDeleted = new Array();
                    var selected = attributeTable.selection.getSelected();
                    for (var i = 0; i < selected.length; ++i) {
                        var item = selected[i];
                        var tag = attributeTable.store.getValue(item, "tag");
                        var value = attributeTable.store.getValue(item, "value");
                        toBeDeleted.push({ tag: tag, value: value });
                    }
                    attributeTable.removeSelectedRows();
                    deleteAttributes(toBeDeleted);
                });         }
            else {
                dojo.style(attributesDiv, "display", "none");
            }

        };
        
        var initPubmedIds = function(feature, config) {
            if (config.hasPubmedIds) {
                var oldPubmedId;
                var pubmedIds = new dojoItemFileWriteStore({
                    data: {
                        items: []
                    }
                });
                for (var i = 0; i < feature.dbxrefs.length; ++ i) {
                    var dbxref = feature.dbxrefs[i];
                    if (dbxref.db == pubmedIdDb) {
                        pubmedIds.newItem({ pubmed_id: dbxref.accession });
                    }
                }
                var pubmedIdTableLayout = [{
                    cells: [
                            {
                                name: 'Pubmed ID',
                                field: 'pubmed_id',
                                width: '100%',
                                formatter: function(pubmedId) {
                                    if (!pubmedId) {
                                        return "Enter new PubMed ID";
                                    }
                                    return pubmedId;
                                },
                                editable: hasWritePermission
                            }
                           ]
                }];

                var pubmedIdTable = new dojoxDataGrid({
                    singleClickEdit: true,
                    store: pubmedIds,
                    updateDelay: 0,
                    structure: pubmedIdTableLayout
                });
                
                var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function() {
                    initTable(pubmedIdTable.domNode, pubmedIdsTable, pubmedIdTable);
                    dojo.disconnect(handle);
                });
                if (reload) {
                    initTable(pubmedIdTable.domNode, pubmedIdsTable, pubmedIdTable, timeout);
                }

                dojo.connect(pubmedIdTable, "onStartEdit", function(inCell, inRowIndex) {
                    oldPubmedId = pubmedIdTable.store.getValue(pubmedIdTable.getItem(inRowIndex), "pubmed_id");
                });
                
                dojo.connect(pubmedIdTable, "onApplyEdit", function(inRowIndex) {
                    var newPubmedId = pubmedIdTable.store.getValue(pubmedIdTable.getItem(inRowIndex), "pubmed_id");
                    if (!newPubmedId) {
                    }
                    else if (!oldPubmedId) {
                        addPubmedId(pubmedIdTable, inRowIndex, newPubmedId);
                    }
                    else {
                        if (newPubmedId != oldPubmedId) {
                            updatePubmedId(pubmedIdTable, inRowIndex, oldPubmedId, newPubmedId);
                        }
                    }
                });

                dojo.connect(addPubmedIdButton, "onclick", function() {
                    pubmedIdTable.store.newItem({ pubmed_id: "" });
                    pubmedIdTable.scrollToRow(pubmedIdTable.rowCount);
                });
                
                dojo.connect(deletePubmedIdButton, "onclick", function() {
                    var toBeDeleted = new Array();
                    var selected = pubmedIdTable.selection.getSelected();
                    for (var i = 0; i < selected.length; ++i) {
                        var item = selected[i];
                        var pubmedId = pubmedIdTable.store.getValue(item, "pubmed_id");
                        toBeDeleted.push({ db: pubmedIdDb, accession: pubmedId });
                    }
                    pubmedIdTable.removeSelectedRows();
                    deletePubmedIds(toBeDeleted);
                });
            }
            else {
                dojo.style(pubmedIdsDiv, "display", "none");
            }
        };
        
        var initGoIds = function(feature, config) {
            if (config.hasGoIds) {
                var oldGoId;
                var dirty = false;
                var valid = true;
                var editingRow = 0;
                var goIds = new dojoItemFileWriteStore({
                    data: {
                        items: []
                    }
                });
                for (var i = 0; i < feature.dbxrefs.length; ++ i) {
                    var dbxref = feature.dbxrefs[i];
                    if (dbxref.db == goIdDb) {
                        goIds.newItem({ go_id: goIdDb + ":" + dbxref.accession });
                    }
                }
                var goIdTableLayout = [{
                    cells: [
                        {
                            name: 'Gene Ontology ID',
                            field: 'go_id', // '_item',
                            width: '100%',
                            type: declare(dojox.grid.cells._Widget, {
                                widgetClass: dijitTextBox,
                                createWidget: function(inNode, inDatum, inRowIndex) {
                                    var widget = new this.widgetClass(this.getWidgetProps(inDatum), inNode);
                                    var textBox = widget.domNode.childNodes[0].childNodes[0];
                                    dojo.connect(textBox, "onkeydown", function(event) {
                                        if (event.keyCode == dojo.keys.ENTER) {
                                            if (dirty) {
                                                dirty = false;
                                                valid = validateGoId(textBox.value) ? true : false;
                                            }
                                        }
                                    });
                                    var gserv = 'http://golr.berkeleybop.org/';
                                    var gconf = new bbop.golr.conf(amigo.data.golr);
                                    var args = {
                                            label_template: '{{annotation_class_label}} [{{annotation_class}}]',
                                            value_template: '{{annotation_class}}',
                                            list_select_callback: function(doc) {
                                                dirty = false;
                                                valid = true;
                                                goIdTable.store.setValue(goIdTable.getItem(editingRow), "go_id", doc.annotation_class);
                                            }
                                    };
                                    var auto = new bbop.widget.search_box(gserv, gconf, textBox, args);
                                    auto.set_personality('bbop_term_ac');
                                    auto.add_query_filter('document_category', 'ontology_class');
                                    auto.add_query_filter('source', '(biological_process OR molecular_function OR cellular_component)');
                                    return widget;
                                }
                            }),
                            formatter: function(goId, rowIndex, cell) {
                                if (!goId) {
                                    return "Enter new Gene Ontology ID";
                                }
                                return goId;
                            },
                            editable: hasWritePermission
                        }
                       ]
                }];

                var goIdTable = new dojoxDataGrid({
                    singleClickEdit: true,
                    store: goIds,
                    updateDelay: 0,
                    structure: goIdTableLayout
                });
                
                var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function() {
                    initTable(goIdTable.domNode, goIdsTable, goIdTable);
                    dojo.disconnect(handle);
                });
                if (reload) {
                    initTable(goIdTable.domNode, goIdsTable, goIdTable, timeout);
                }
                
                dojo.connect(goIdTable, "onStartEdit", function(inCell, inRowIndex) {
                    editingRow = inRowIndex;
                    oldGoId = goIdTable.store.getValue(goIdTable.getItem(inRowIndex), "go_id");
                    dirty = true;
                });
                
                // dojo.connect(goIdTable, "onApplyCellEdit", function(inValue, inRowIndex, inCellIndex) {
                dojo.connect(goIdTable.store, "onSet", function(item, attribute, oldValue, newValue) {
                    if (dirty) {
                        return;
                    }
                    // var newGoId = goIdTable.store.getValue(goIdTable.getItem(inRowIndex),
                    // "go_id");
                    var newGoId = newValue;
                    if (!newGoId) {
                    }
                    else if (!oldGoId) {
                        addGoId(goIdTable, editingRow, newGoId, valid);
                    }
                    else {
                        if (newGoId != oldGoId) {
                            // updateGoId(goIdTable, editingRow, oldGoId, newGoId);
                            updateGoId(goIdTable, item, oldGoId, newGoId, valid);
                        }
                    }
                    goIdTable.render();
                });
                                    
                dojo.connect(addGoIdButton, "onclick", function() {
                    goIdTable.store.newItem({ go_id: "" });
                    goIdTable.scrollToRow(goIdTable.rowCount);
                });
                
                dojo.connect(deleteGoIdButton, "onclick", function() {
                    var toBeDeleted = new Array();
                    var selected = goIdTable.selection.getSelected();
                    for (var i = 0; i < selected.length; ++i) {
                        var item = selected[i];
                        var goId = goIdTable.store.getValue(item, "go_id");
                        toBeDeleted.push({ db: goIdDb, accession: goId.substr(goIdDb.length + 1) });
                    }
                    goIdTable.removeSelectedRows();
                    deleteGoIds(toBeDeleted);
                });             
            }
            else {
                dojo.style(goIdsDiv, "display", "none");
            }
        };
        
        var initComments = function(feature, config) {
            if (config.hasComments) {
                var cannedComments = feature.canned_comments;
                var oldComment;
                var comments = new dojoItemFileWriteStore({
                    data: {
                        items: []
                    }
                });
                for (var i = 0; i < feature.comments.length; ++ i) {
                    var comment = feature.comments[i];
                    comments.newItem({ comment: comment });
                }
                var commentTableLayout = [{
                    cells: [
                            {
                                name: 'Comment',
                                field: 'comment',
                                editable: hasWritePermission,
                                type: dojox.grid.cells.ComboBox, 
                                options: cannedComments,
                                formatter: function(comment) {
                                    if (!comment) {
                                        return "Enter new comment";
                                    }
                                    return comment;
                                },
                                width: "100%"
                            }
                           ]
                }];
                var commentTable = new dojoxDataGrid({
                    singleClickEdit: true,
                    store: comments,
                    structure: commentTableLayout,
                    updateDelay: 0
                });
                
                var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function() {
                    initTable(commentTable.domNode, commentsTable, commentTable);
                    dojo.disconnect(handle);
                });
                if (reload) {
                    initTable(commentTable.domNode, commentsTable, commentTable, timeout);
                }

                dojo.connect(commentTable, "onStartEdit", function(inCell, inRowIndex) {
                    oldComment = commentTable.store.getValue(commentTable.getItem(inRowIndex), "comment");
                });
                
                dojo.connect(commentTable, "onApplyCellEdit", function(inValue, inRowIndex, inFieldIndex) {
                    var newComment = inValue;
                    if (!newComment) {
                        // alert("No comment");
                    }
                    else if (!oldComment) {
                        addComment(newComment);
                    }
                    else {
                        if (newComment != oldComment) {
                            updateComment(oldComment, newComment);
                        }
                    }
                });
                
                dojo.connect(addCommentButton, "onclick", function() {
                    commentTable.store.newItem({ comment: undefined });
                    commentTable.scrollToRow(commentTable.rowCount);
                });
                
                dojo.connect(deleteCommentButton, "onclick", function() {
                    var toBeDeleted = new Array();
                    var selected = commentTable.selection.getSelected();
                    for (var i = 0; i < selected.length; ++i) {
                        var comment = commentTable.store.getValue(selected[i], "comment");
                        toBeDeleted.push(comment);
                    }
                    commentTable.removeSelectedRows();
                    deleteComments(toBeDeleted);
                });
            }
            else {
                dojo.style(commentsDiv, "display", "none");
            }
        };
        
        var processOtherMetadata = function() {
            var config = track.annotationInfoEditorConfigs[featureType] || track.annotationInfoEditorConfigs["default"];
            var status = config.status;
            var maxLength = 0;
            if (status) {
                for (var i = 0; i < status.length; ++i) {
                    if (status[i].length > maxLength) {
                        maxLength = status[i].length;
                    }
                }
                for (var i = 0; i < status.length; ++i) {
                    var statusRadioDiv = dojo.create("span", { class: "annotation_info_editor_radio", style: "width:" + (maxLength * 0.75) + "em;" }, statusFlags);
                    var statusRadio = new dijitRadioButton({ value: status[i], name: "status_" + uniqueName });
                    if (!hasWritePermission) {
                        statusRadio.set("disabled", true);
                    }
                    dojo.place(statusRadio.domNode, statusRadioDiv);
                    var statusLabel = dojo.create("label", { innerHTML: status[i], class: "annotation_info_editor_radio_label" }, statusRadioDiv);
                    statusRadios[status[i]] = statusRadio;
                    dojo.connect(statusRadio, "onMouseDown", function(div, radio, label) {
                        return function(event) {
                            if (radio.checked) {
                                deleteStatus();
                                dojo.place(new dijitRadioButton({ value: status[i], name: "status_" + uniqueName, checked: false }).domNode, radio.domNode, "replace");
                            }
                        };
                    }(statusRadioDiv, statusRadio, statusLabel));
                    dojo.connect(statusRadio, "onChange", function(label) {
                        return function(selected) {
                            if (selected && hasWritePermission) {
                                updateStatus(label);
                            }
                        };
                    }(status[i]));
                }
                getStatus();
            }
            else {
                dojo.style(statusDiv, "display", "none");
            }
            config.hasDbxrefs ? getDbxrefs() : dojo.style(dbxrefsDiv, "display", "none");
            config.hasAttributes ? getAttributes() : dojo.style(attributesDiv, "display", "none");
            config.hasPubmedIds ? getPubmedIds() : dojo.style(pubmedIdsDiv, "display", "none");
            config.hasGoIds ? getGoIds() : dojo.style(goIdsDiv, "display", "none");
            if (config.hasComments) {
                getCannedComments();
                getComments();
            }
            else {
                dojo.style(commentsDiv, "display", "none");
            }

        };
        
        function updateTimeLastUpdated() {
            var date = new Date();
            dateLastModifiedField.set("value", FormatUtils.formatDate(date.getTime()));
        };
        
        var updateName = function(name) {
            name = escapeString(name);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "name": "' + name + '" } ]';
            var operation = "set_name";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var updateSymbol = function(symbol) {
            symbol = escapeString(symbol);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "symbol": "' + symbol + '" } ]';
            var operation = "set_symbol";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };
        
        var updateDescription = function(description) {
            description = escapeString(description);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "description": "' + description + '" } ]';
            var operation = "set_description";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };
        
        var deleteStatus = function() {
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "status": "' + status + '" } ]';
            var operation = "delete_status";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };
        
        var updateStatus = function(status) {
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "status": "' + status + '" } ]';
            var operation = "set_status";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };
    
        var addDbxref = function(db, accession) {
            db = escapeString(db);
            accession = escapeString(accession);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": [ { "db": "' + db + '", "accession": "' + accession + '" } ] } ]';
            var operation = "add_non_primary_dbxrefs";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var deleteDbxrefs = function(dbxrefs) {
            for (var i = 0; i < dbxrefs.length; ++i) {
                dbxrefs[i].accession = escapeString(dbxrefs[i].accession);
                dbxrefs[i].db = escapeString(dbxrefs[i].db);
            }
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": ' + JSON.stringify(dbxrefs) + ' } ]';
            var operation = "delete_non_primary_dbxrefs";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var updateDbxref = function(oldDb, oldAccession, newDb, newAccession) {
            oldDb = escapeString(oldDb);
            oldAccession = escapeString(oldAccession);
            newDb = escapeString(newDb);
            newAccession = escapeString(newAccession);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "old_dbxrefs": [ { "db": "' + oldDb + '", "accession": "' + oldAccession + '" } ], "new_dbxrefs": [ { "db": "' + newDb + '", "accession": "' + newAccession + '" } ] } ]';
            var operation = "update_non_primary_dbxrefs";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var addAttribute = function(tag, value) {
            tag = escapeString(tag);
            value = escapeString(value);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "non_reserved_properties": [ { "tag": "' + tag + '", "value": "' + value + '" } ] } ]';
            var operation = "add_non_reserved_properties";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var deleteAttributes = function(attributes) {
            for (var i = 0; i < attributes.length; ++i) {
                attributes[i].tag = escapeString(attributes[i].tag);
                attributes[i].value = escapeString(attributes[i].value);
            }
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "non_reserved_properties": ' + JSON.stringify(attributes) + ' } ]';
            var operation = "delete_non_reserved_properties";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var updateAttribute = function(oldTag, oldValue, newTag, newValue) {
            oldTag = escapeString(oldTag);
            oldValue = escapeString(oldValue);
            newTag = escapeString(newTag);
            newValue = escapeString(newValue);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "old_non_reserved_properties": [ { "tag": "' + oldTag + '", "value": "' + oldValue + '" } ], "new_non_reserved_properties": [ { "tag": "' + newTag + '", "value": "' + newValue + '" } ] } ]';
            var operation = "update_non_reserved_properties";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var confirmPubmedEntry = function(record) {
            return confirm("Publication title: '" + record.PubmedArticleSet.PubmedArticle.MedlineCitation.Article.ArticleTitle + "'");
        };

        var addPubmedId = function(pubmedIdTable, row, pubmedId) {
            var eutils = new EUtils(context_path, track.handleError);
            var record = eutils.fetch("pubmed", pubmedId);
            if (record) {
                // if (eutils.validateId("pubmed", pubmedId)) {
                if (confirmPubmedEntry(record)) {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": [ { "db": "' + pubmedIdDb + '", "accession": "' + pubmedId + '" } ] } ]';
                    var operation = "add_non_primary_dbxrefs";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                }
                else {
                    pubmedIdTable.store.deleteItem(pubmedIdTable.getItem(row));
                }
            }
            else {
                alert("Invalid ID " + pubmedId + " - Removing entry");
                pubmedIdTable.store.deleteItem(pubmedIdTable.getItem(row));
            }
            // EUtils.validateId("pubmed", pubmedId, function() {
                /*
                 * var features = '"features": [ { "uniquename": "' + uniqueName +
                 * '", "dbxrefs": [ { "db": "' + pubmedIdDb + '", "accession": "' +
                 * pubmedId + '" } ] } ]'; var operation =
                 * "add_non_primary_dbxrefs"; var postData = '{ "track": "' +
                 * trackName + '", ' + features + ', "operation": "' + operation + '"
                 * }'; track.executeUpdateOperation(postData);
                 */
            /*
             * }, function(message) {
             * pubmedIdTable.store.deleteItem(pubmedIdTable.getItem(row)); //
             * pubmedIdTable.doStartEdit(pubmedIdTable.getItem(row), row);
             * alert(message + " - Removing entry"); //
             * pubmedIdTable.edit.setEditCell(pubmedIdTable.getCell(0), row); //
             * pubmedIdTable.edit.cellFocus(pubmedIdTable.getCell(0), row ); //
             * pubmedIdTable.doStartEdit(pubmedIdTable.layout.cells[row], row);
             * });
             */
        };

        var deletePubmedIds = function(pubmedIds) {
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": ' + JSON.stringify(pubmedIds) + ' } ]';
            var operation = "delete_non_primary_dbxrefs";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var updatePubmedId = function(pubmedIdTable, row, oldPubmedId, newPubmedId) {
            var eutils = new EUtils(context_path, track.handleError);
            var record = eutils.fetch("pubmed", newPubmedId);
            // if (eutils.validateId("pubmed", newPubmedId)) {
            if (record) {
                if (confirmPubmedEntry(record)) {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "old_dbxrefs": [ { "db": "' + pubmedIdDb + '", "accession": "' + oldPubmedId + '" } ], "new_dbxrefs": [ { "db": "' + pubmedIdDb + '", "accession": "' + newPubmedId + '" } ] } ]';
                    var operation = "update_non_primary_dbxrefs";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                }
                else {
                    pubmedIdTable.store.setValue(pubmedIdTable.getItem(row), "pubmed_id", oldPubmedId);
                }
            }
            else {
                alert("Invalid ID " + newPubmedId + " - Undoing update");
                pubmedIdTable.store.setValue(pubmedIdTable.getItem(row), "pubmed_id", oldPubmedId);
            }
        };
        
        var validateGoId = function(goId) {
            var regex = new RegExp("^" + goIdDb + ":(\\d{7})$");
            return regex.exec(goId);
        };
        
        var addGoId = function(goIdTable, row, goId, valid) {
        // if (match = validateGoId(goId)) {
            if (valid) {
                var goAccession = goId.substr(goIdDb.length + 1);
                var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": [ { "db": "' + goIdDb + '", "accession": "' + goAccession + '" } ] } ]';
                var operation = "add_non_primary_dbxrefs";
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
                updateTimeLastUpdated();
            }
            else {
                alert("Invalid ID " + goId + " - Must be formatted as 'GO:#######' - Removing entry");
                goIdTable.store.deleteItem(goIdTable.getItem(row));
            }
        };

        var deleteGoIds = function(goIds) {
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": ' + JSON.stringify(goIds) + ' } ]';
            var operation = "delete_non_primary_dbxrefs";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

// var updateGoId = function(goIdTable, row, oldGoId, newGoId) {
        var updateGoId = function(goIdTable, item, oldGoId, newGoId, valid) {
// if (match = validateGoId(newGoId)) {
            if (valid) {
                var oldGoAccession = oldGoId.substr(goIdDb.length + 1);
                var newGoAccession = newGoId.substr(goIdDb.length + 1);
                var features = '"features": [ { "uniquename": "' + uniqueName + '", "old_dbxrefs": [ { "db": "' + goIdDb + '", "accession": "' + oldGoAccession + '" } ], "new_dbxrefs": [ { "db": "' + goIdDb + '", "accession": "' + newGoAccession + '" } ] } ]';
                var operation = "update_non_primary_dbxrefs";
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
                updateTimeLastUpdated();
            }
            else {
                alert("Invalid ID " + newGoId + " - Undoing update");
// goIdTable.store.setValue(goIdTable.getItem(row), "go_id", oldGoId);
                goIdTable.store.setValue(item, "go_id", oldGoId);
            }
        };
        
        var addComment = function(comment) {
            comment = escapeString(comment);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "comments": [ "' + comment + '" ] } ]';
            var operation = "add_comments";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var deleteComments = function(comments) {
            for (var i = 0; i < comments.length; ++i) {
                comments[i] = escapeString(comments[i]);
            }
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "comments": ' + JSON.stringify(comments) + ' } ]';
            var operation = "delete_comments";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var updateComment = function(oldComment, newComment) {
            if (oldComment == newComment) {
                return;
            }
            oldComment = escapeString(oldComment);
            newComment = escapeString(newComment);
            var features = '"features": [ { "uniquename": "' + uniqueName + '", "old_comments": [ "' + oldComment + '" ], "new_comments": [ "' + newComment + '"] } ]';
            var operation = "update_comments";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            track.executeUpdateOperation(postData);
            updateTimeLastUpdated();
        };

        var getCannedComments = function() {
            var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
            var operation = "get_canned_comments";
            var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
            dojo.xhrPost( {
                postData: postData,
                url: context_path + "/AnnotationEditorService",
                handleAs: "json",
                sync: true,
                timeout: 5000 * 1000, // Time in milliseconds
                load: function(response, ioArgs) {
                    var feature = response.features[0];
                        cannedComments = feature.comments;
                    },
                    // The ERROR function will be called in an error case.
                error: function(response, ioArgs) {
                    track.handleError(response);
                    console.error("HTTP status code: ", ioArgs.xhr.status);
                    return response;
                }
            });
        };
        
        init();
        return content;
    }
});

});