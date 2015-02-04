define( [
            'dojo/_base/declare',
            'jquery',
            'jqueryui/droppable',
            'jqueryui/resizable',
            'jqueryui/draggable',
            'dijit/Menu',
            'dijit/MenuItem', 
            'dijit/MenuSeparator', 
            'dijit/form/Button',
            'dijit/form/DropDownButton',
            'dojox/widget/DialogSimple',
            'dojo/json',
            'WebApollo/View/Track/DraggableHTMLFeatures',
            'WebApollo/FeatureSelectionManager',
            'WebApollo/JSONUtils',
            'WebApollo/Permission', 
            'WebApollo/SequenceSearch', 
            'WebApollo/SequenceOntologyUtils',
            'JBrowse/Model/SimpleFeature',
            'JBrowse/Util', 
            'JBrowse/View/GranularRectLayout',
            'dojo/request/xhr',
            'dojox/widget/Standby',
            'dijit/Tooltip',
            'WebApollo/FormatUtils',
            'WebApollo/View/InformationEditor',
            'WebApollo/View/History'
            'WebApollo/View/GetSequence'
        ],
        function( declare,
                $,
                droppable,
                resizable,
                draggable,
                dijitMenu,
                dijitMenuItem,
                dijitMenuSeparator,
                dijitButton,
                dijitDropDownButton,
                dojoxDialogSimple,
                JSON,
                DraggableFeatureTrack,
                FeatureSelectionManager,
                JSONUtils,
                Permission,
                SequenceSearch,
                SequenceOntologyUtils,
                SimpleFeature,
                Util,
                Layout,
                xhr,
                Standby,
                Tooltip,
                FormatUtils,
                InformationEditorMixin,
                HistoryMixin,
                GetSequenceMixin
                ) {



var annot_context_menu;
var contextMenuItems;

var context_path = "..";


var AnnotTrack = declare([DraggableFeatureTrack,InformationEditorMixin,HistoryMixin,GetSequenceMixin], 
{
    constructor: function( args ) {
        this.has_custom_context_menu = true;
        this.exportAdapters = [];

        this.selectionManager = this.setSelectionManager( this.webapollo.annotSelectionManager );

        this.selectionClass = "selected-annotation";
        this.annot_under_mouse = null;


        var thisObj = this;
        this.comet_working = true;


        this.verbose_create = false;
        this.verbose_add = false;
        this.verbose_delete = false;
        this.verbose_drop = false;
        this.verbose_click = false;
        this.verbose_resize = false;
        this.verbose_mousedown = false;
        this.verbose_mouseenter = false;
        this.verbose_mouseleave = false;
        this.verbose_render = false;
        this.verbose_server_notification = false;

        var track = this;


        this.gview.browser.subscribe("/jbrowse/v1/n/navigate", dojo.hitch(this, function(currRegion) {
            if (currRegion.ref != this.refSeq.name) {
                if (this.listener && this.listener.fired == -1 ) {
                    this.listener.cancel();
                }
            }
        }));

        this.gview.browser.subscribe("/jbrowse/v1/v/tracks/show", dojo.hitch(this, function(names) {
        }));
        
        if(!this.gview.browser._keyBoardShortcuts)
        {
            this.gview.browser.setGlobalKeyboardShortcut('[', track, 'scrollToPreviousEdge');
            this.gview.browser.setGlobalKeyboardShortcut(']', track, 'scrollToNextEdge');
        
            this.gview.browser.setGlobalKeyboardShortcut('}', track, 'scrollToNextTopLevelFeature');
            this.gview.browser.setGlobalKeyboardShortcut('{', track, 'scrollToPreviousTopLevelFeature');
            this.gview.browser._keyBoardShortcuts=true;
        }
        
        this.topLevelParents = {};
    },

    renderExonSegments: function( subfeature, subDiv, cdsMin, cdsMax, displayStart, displayEnd, priorCdsLength, reverse)  {
        var utrClass;
        var parentType = subfeature.parent().afeature.parent_type;
        if (!this.isProteinCoding(subfeature.parent())) {
            // utrClass = parentType && parentType.name == "pseudogene" ? "pseudogene" :
            // subfeature.parent().get("type");
            var clsName = parentType && parentType.name == "pseudogene" ? "pseudogene" : subfeature.parent().get("type");
            var cfg = this.config.style.alternateClasses[clsName];
            utrClass = cfg.className;
        }
        return this.inherited(arguments,[subfeature, subDiv, cdsMin, cdsMax, displayStart, displayEnd, priorCdsLength, reverse, utrClass]);
    },

    _defaultConfig: function() {
        var thisConfig = this.inherited(arguments);
        // nulling out menuTemplate to suppress default JBrowse feature contextual
        // menu
        thisConfig.menuTemplate = null;
        thisConfig.noExport = true;  // turn off default "Save track data" "
        thisConfig.style.centerChildrenVertically = false;
        thisConfig.pinned = true;
        return thisConfig;
    },

    /**
     * removing "Pin to top" menuitem, so SequenceTrack is always pinned and
     * "Delete track" menuitem, so can't be deleted (very hacky since depends on
     * label property of menuitem config)
     */
    _trackMenuOptions: function() {
        var options = this.inherited( arguments );
        options = this.webapollo.removeItemWithLabel(options, "Pin to top");
        options = this.webapollo.removeItemWithLabel(options, "Delete track");
        return options;
    }, 
    
    setViewInfo: function( genomeView, numBlocks,
                           trackDiv, labelDiv,
                           widthPct, widthPx, scale ) {
                   
        this.inherited( arguments );
        var track = this;
        track.hide();
        this.getPermission( ).then(function() {
            var standby = new Standby({target: track.div, color: "transparent",image: "plugins/WebApollo/img/loading.gif"});
            document.body.appendChild(standby.domNode);
            standby.startup();
            standby.show();

            track.initAnnotContextMenu();

            track.initSaveMenu();
            track.initPopupDialog();

            track.createAnnotationChangeListener();
            xhr(context_path + "/AnnotationEditorService", {
                handleAs: "json",
                data: JSON.stringify({ "track": track.getUniqueTrackName(), "operation": "get_features" }),
                method: "post"
            }).then(function(response, ioArgs) {
                var responseFeatures = response.features;
                var i = 0;

                var func = function() {
                    while (i < responseFeatures.length) {
                        var jfeat = JSONUtils.createJBrowseFeature( responseFeatures[i] );
                        track.store.insert(jfeat);
                        track.processParent(responseFeatures[i], "ADD");
                        if ((i++ % 100) == 0) {
                            window.setTimeout(func, 1);
                            return;
                        }
                    }
                    if (i == responseFeatures.length) {
                        track.changed();
                        standby.hide();
                    }
                };
                func();
            }, function(response, ioArgs) { //
                console.log("Annotation server error--maybe you forgot to login to the server?");
                track.handleError({ responseText: response.response.text } );
                return response; //
            });

            track.makeTrackDroppable();
            track.show();

            // initialize menus regardless
            if (!track.webapollo.loginMenuInitialized) {
                track.webapollo.initLoginMenu(track.username);
            }
            if (! track.webapollo.searchMenuInitialized && track.permission)  {
                track.webapollo.initSearchMenu();
            }
        },
        function() {
            if(track.config.disableJBrowseMode) {
                track.login();
            }
            if (!track.webapollo.loginMenuInitialized) {
                track.webapollo.initLoginMenu(track.username);
            }
        });


    }, 

    createAnnotationChangeListener: function(retryNumber) {
        var track = this;
        if (retryNumber === undefined) {
            retryNumber = 0;
        }
        // server error if tried connecting 5 times and failed
        if (retryNumber > 5) {
            track.handleError({responseText: '{ error: "Server connection error" }'});
            window.location.reload();
            return;
        }

        this.listener = dojo.xhrGet( {
            url: context_path + "/AnnotationChangeNotificationService",
            content: {
                track: track.getUniqueTrackName()
            },
            handleAs: "json",
            preventCache: true,
            timeout: 5 * 60 * 1000,
            failOk: true,
            load: function(response, ioArgs) {
                if (response == null) {
                    track.createAnnotationChangeListener();
                }
                else {
                    for (var i in response) {
                        var changeData = response[i];
                        if (track.verbose_server_notification) {
                            console.log(changeData.operation + " command from server: ");
                            console.log(changeData);
                        }
                        if (changeData.operation == "ADD") {
                            if (changeData.sequenceAlterationEvent) {
                                    track.getSequenceTrack().annotationsAddedNotification(changeData.features);
                            }
                            else {
                                    track.annotationsAddedNotification(changeData.features);
                            }
                        }
                        else if (changeData.operation == "DELETE") {
                            if (changeData.sequenceAlterationEvent) {
                                    track.getSequenceTrack().annotationsDeletedNotification(changeData.features);
                            }
                            else {
                                    track.annotationsDeletedNotification(changeData.features);
                            }
                        }
                        else if (changeData.operation == "UPDATE") {
                            if (changeData.sequenceAlterationEvent) {
                                track.getSequenceTrack().annotationsUpdatedNotification(changeData.features);
                            }
                            else {
                                track.annotationsUpdatedNotification(changeData.features);
                            }
                        }
                        else  {
                            // unknown command from server, null-op?
                        }
                    }
                    track.changed();
                    track.createAnnotationChangeListener();
                }
            },
            error: function(response, ioArgs) { //
                // client cancel
                if (response.dojoType == "cancel") {
                    return;
                }
                // client timeout
                if (response.dojoType == "timeout") {
                    setTimeout(function() { track.createAnnotationChangeListener(++retryNumber); }, 300 * retryNumber );
                    return;
                }
                if (ioArgs.xhr.status == 0) {
                    return;
                }
                // bad gateway
                else if (ioArgs.xhr.status == 502) {
                    console.log("502 received");
                    setTimeout(function() { track.createAnnotationChangeListener(++retryNumber); }, 300 * retryNumber );
                    return;
                }
                // server killed
                else if (ioArgs.xhr.status == 503) {
                    console.log("503 received");
                    window.location.reload();
                    return;
                }
                
                // server timeout
                else if (ioArgs.xhr.status == 504){
                    console.log("504 received");
                    setTimeout(function() { track.createAnnotationChangeListener(++retryNumber); }, 300 * retryNumber );
                    return;
                }
                // forbidden
                else if (ioArgs.xhr.status == 403) {
                    track.hide();
                    track.changed();
                    track.handleError({responseText: '{ error: "Logged out" }'});
                    window.location.reload();
                    return;
                }
                // actual error
                if (response.responseText) {
                    track.handleError(response);
                    track.comet_working = false;
                    console.error("HTTP status code: ", ioArgs.xhr.status);
                    return response;
                }
                // everything else
                else {
                    track.handleError({responseText: '{ error: "Server connection error" }'});
                    return;
                }
                
            }
        });
    },

    /**
     * received notification from server ChangeNotificationListener that
     * annotations were added
     */
    annotationsAddedNotification: function(responseFeatures) {
        for (var i = 0; i < responseFeatures.length; ++i) {
            var feat = JSONUtils.createJBrowseFeature( responseFeatures[i] );
            var id = responseFeatures[i].uniquename;
            if (! this.store.getFeatureById(id))  {
                this.store.insert(feat);
                this.processParent(responseFeatures[i], "ADD");
            }
        }
    },

    /**
     * received notification from server ChangeNotificationListener that
     * annotations were deleted
     */
    annotationsDeletedNotification: function(responseFeatures) {
        for (var i = 0; i < responseFeatures.length; ++i) {
            var id_to_delete = responseFeatures[i].uniquename;
            this.store.deleteFeatureById(id_to_delete);
            this.processParent(responseFeatures[i], "DELETE");
        }
    },

    /*
     * received notification from server ChangeNotificationListener that
     * annotations were updated currently handled as if receiving DELETE
     * followed by ADD command
     */
    annotationsUpdatedNotification: function(responseFeatures)  {
        var selfeats = this.selectionManager.getSelectedFeatures();
    
        for (var i = 0; i < responseFeatures.length; ++i) {
            var id = responseFeatures[i].uniquename;
            var feat = JSONUtils.createJBrowseFeature( responseFeatures[i] );
            this.store.replace(feat);
            this.processParent(responseFeatures[i], "UPDATE");
        }
    },

    /**
     * overriding renderFeature to add event handling right-click context menu
     */
    renderFeature:  function( feature, uniqueId, block, scale, labelScale, descriptionScale, 
                              containerStart, containerEnd, history ) {
        var track = this;

        var rclass;
        var clsName;
        var type = feature.afeature.type;
        if (!this.isProteinCoding(feature)) {
            var topLevelAnnotation = this.getTopLevelAnnotation(feature);
            var parentType = feature.afeature.parent_type ? feature.afeature.parent_type.name : null;
            var cfg = this.config.style.alternateClasses[feature.get("type")] || this.config.style.alternateClasses[parentType];
            if (cfg) {
                rclass = cfg.renderClassName;
                if (!topLevelAnnotation.afeature.parent_type) {
                    clsName = cfg.className;
                }
            }
        }
        var featDiv = this.inherited(arguments,[feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd, rclass, clsName]);

        if (featDiv && featDiv != null && !history)  {
            annot_context_menu.bindDomNode(featDiv);
            $(featDiv).droppable(  {
                accept: ".selected-feature",
                tolerance: "pointer",
                hoverClass: "annot-drop-hover",
                over: function(event, ui)  {
                    track.annot_under_mouse = event.target;
                },
                out: function(event, ui)  {
                    track.annot_under_mouse = null;
                },
                drop: function(event, ui)  {
                    
                    if (track.verbose_drop)  {
                        console.log("dropped feature on annot:");
                        console.log(featDiv);
                    }
                }
            } )
                .click(function(event){
                    if (event.altKey) {
                        track.getAnnotationInfoEditor(track);
                    }
                })
            ;
        }
        
        if (!history) {
            var label = "Type: " + type.name + "<br/>Owner: " + feature.get("owner") + "<br/>Last modified: " + FormatUtils.formatDate(feature.afeature.date_last_modified) + " " + FormatUtils.formatTime(feature.afeature.date_last_modified);
            if (feature.get("locked")) {
                label += "<br/>[Locked]";
            }
            new Tooltip({
                connectId: featDiv,
                label: label,
                position: ["above"],
                showDelay: 600
            });
        }
        
        if (feature.get("locked")) {
            dojo.addClass(featDiv, "locked-annotation");
        }
        
        return featDiv;
    },

    renderSubfeature: function( feature, featDiv, subfeature, displayStart, displayEnd, block) {
        var subdiv = this.inherited( arguments );

        if (this.canEdit(feature)) {
            /**
             * setting up annotation resizing via pulling of left/right edges but if
             * subfeature is not selectable, do not bind mouse down
             */
            if (subdiv && subdiv != null && (! this.selectionManager.unselectableTypes[subfeature.get('type')]) )  {
                $(subdiv).bind("mousedown", dojo.hitch(this, 'onAnnotMouseDown'));
            }
        }
        
        return subdiv;
    },

    /**
     * get the GenomeView's sequence track -- maybe move this to GenomeView?
     * WebApollo assumes there is only one SequenceTrack if there are multiple
     * SequenceTracks, getSequenceTrack returns first one found iterating
     * through tracks list
     */
    getSequenceTrack: function()  {
        var thisB=this;
        if (this.seqTrack)  {
            return this.seqTrack;
        }
        else  {
            array.some(this.gview.tracks,function(track) {
                if (track.isInstanceOf(SequenceTrack))  {
                    thisB.seqTrack=track;
                    return true;
                }
            });
        }
        return this.seqTrack||{};
    }, 

    onFeatureMouseDown: function(event) {

        this.last_mousedown_event = event;
        var ftrack = this;
        if (ftrack.verbose_selection || ftrack.verbose_drag)  {
            console.log("AnnotTrack.onFeatureMouseDown called, genome coord: " + this.getGenomeCoord(event));
        }

        this.handleFeatureSelection(event);
    },

    /**
     * handles mouse down on an annotation subfeature to make the annotation
     * resizable by pulling the left/right edges
     */
    onAnnotMouseDown: function(event)  {
        var track = this;
        // track.last_mousedown_event = event;
        var verbose_resize = track.verbose_resize;
        if (verbose_resize || track.verbose_mousedown)  { console.log("AnnotTrack.onAnnotMouseDown called"); }
        event = event || window.event;
        var elem = (event.currentTarget || event.srcElement);
        var featdiv = track.getLowestFeatureDiv(elem);

        this.currentResizableFeature = featdiv.subfeature;
        this.makeResizable(featdiv);
        event.stopPropagation();
    },

    makeResizable: function(featdiv) {
        var track = this;
        var verbose_resize = this.verbose_resize;
        if (featdiv && (featdiv != null))  {
            if (dojo.hasClass(featdiv, "ui-resizable"))  {
                if (verbose_resize) {
                    console.log("already resizable");
                    console.log(featdiv);
                }
            }
            else {
                if (verbose_resize)  {
                    console.log("making annotation resizable");
                    console.log(featdiv);
                }
                var scale = track.gview.bpToPx(1);
                
                var gridvals=[scale];
                $(featdiv).resizable( {
                    handles: "e, w",
                    helper: "ui-resizable-helper",
                    autohide: false,
                    grid: gridvals,

                    stop: function(event, ui)  {
                        if( verbose_resize ) {
                            console.log("resizable.stop() called, event:");
                            console.dir(event);
                            console.log("ui:");
                            console.dir(ui);
                        }
                        var oldPos = ui.originalPosition;
                        var newPos = ui.position;
                        var oldSize = ui.originalSize;
                        var newSize = ui.size;
                        var leftDeltaPixels = newPos.left - oldPos.left;
                        var leftDeltaBases = Math.round(track.gview.pxToBp(leftDeltaPixels));
                        var oldRightEdge = oldPos.left + oldSize.width;
                        var newRightEdge = newPos.left + newSize.width;
                        var rightDeltaPixels = newRightEdge - oldRightEdge;
                        var rightDeltaBases = Math.round(track.gview.pxToBp(rightDeltaPixels));
                        if (verbose_resize)  {
                            console.log("left edge delta pixels: " + leftDeltaPixels);
                            console.log("left edge delta bases: " + leftDeltaBases);
                            console.log("right edge delta pixels: " + rightDeltaPixels);
                            console.log("right edge delta bases: " + rightDeltaBases);
                        }
                        var subfeat = ui.originalElement[0].subfeature;

                        var fmin = subfeat.get('start') + leftDeltaBases;
                        var fmax = subfeat.get('end') + rightDeltaBases;
                        var operation = subfeat.get("type") == "exon" ? "set_exon_boundaries" : "set_boundaries";
                        var postData = { 
                            "track": track.getUniqueTrackName(), 
                            "features": [ { 
                                "uniquename": subfeat.getUniqueName(),
                                "location": {
                                    "fmin": fmin,
                                    "fmax": fmax
                                }
                            } ],
                            "operation":operation
                        };
                        track.executeUpdateOperation(postData);
                        track.changed();
                    }
                } );
            }
        }
    },
    
    /**
     * feature click no-op (to override FeatureTrack.onFeatureClick, which
     * conflicts with mouse-down selection
     */
    onFeatureClick: function(event) {

        if (this.verbose_click)  { console.log("in AnnotTrack.onFeatureClick"); }
        event = event || window.event;
        var elem = (event.currentTarget || event.srcElement);
        var featdiv = this.getLowestFeatureDiv( elem );
        if (featdiv && (featdiv != null))  {
            if (this.verbose_click)  { console.log(featdiv); }
        }
    },

    addToAnnotation: function(annot, feature_records)  {
        var target_track = this;

        var subfeats = [];
        var allSameStrand = 1;
        for (var i = 0; i < feature_records.length; ++i)  { 
            var feature_record = feature_records[i];
            var original_feat = feature_record.feature;
            var feat = JSONUtils.makeSimpleFeature( original_feat );
            var isSubfeature = !! feat.parent();
            var annotStrand = annot.get('strand');
            if (isSubfeature)  {
                var featStrand = feat.get('strand');
                var featToAdd = feat;
                if (featStrand != annotStrand) {
                        allSameStrand = 0;
                        featToAdd.set('strand', annotStrand);
                }
                subfeats.push(featToAdd);
            }
            else  {  // top-level feature
                var source_track = feature_record.track;
                var subs = feat.get('subfeatures');
                if ( subs && subs.length > 0 ) {
                    for (var i = 0; i < subs.length; ++i) {
                        var subfeat = subs[i];
                        var featStrand = subfeat.get('strand');
                        var featToAdd = subfeat;
                        if (featStrand != annotStrand) {
                            allSameStrand = 0;
                            featToAdd.set('strand', annotStrand);
                        }
                        subfeats.push(featToAdd);
                    }
                }
                else  {  // top-level feature without subfeatures
                    // make exon feature
                    var featStrand = feat.get('strand');
                    var featToAdd = feat;
                    if (featStrand != annotStrand) {
                            allSameStrand = 0;
                            featToAdd.set('strand', annotStrand);
                    }
                    featToAdd.set('type', 'exon');
                    subfeats.push(featToAdd);
                }
            }
        }

        if (!allSameStrand && !confirm("Adding features of opposite strand.  Continue?")) {
            return;
        }

        var features = [];
        features.push({"uniquename": annot.id()});

        for (var i = 0; i < subfeats.length; ++i) {
            var subfeat = subfeats[i];
            var source_track = subfeat.track;
            if ( subfeat.get('type') != "wholeCDS") {
                var jsonFeature = JSONUtils.createApolloFeature( subfeats[i], "exon");
                features.push(jsonFeature );
            }
        }
        var postData = { "track":target_track.getUniqueTrackName(), "features": features, "operation": "add_exon" };
        target_track.executeUpdateOperation(postData);
    },

    makeTrackDroppable: function() {
        var target_track = this;
        var target_trackdiv = target_track.div;
        if (target_track.verbose_drop)  {
            console.log("making track a droppable target: ");
            console.log(this);
            console.log(target_trackdiv);
        }
        $(target_trackdiv).droppable(  {
            // only accept draggables that are selected feature divs
            accept: ".selected-feature",   
            
            over: function(event, ui) {
                target_track.track_under_mouse_drag = true;
                if (target_track.verbose_drop) { console.log("droppable entered AnnotTrack") };
            },
            out: function(event, ui) {
                target_track.track_under_mouse_drag = false;
                if (target_track.verbose_drop) { console.log("droppable exited AnnotTrack") };
            },
            deactivate: function(event, ui)  {
                if (target_track.verbose_drop)  { console.log("draggable deactivated"); }

                var dropped_feats = target_track.webapollo.featSelectionManager.getSelection();
                // problem with making individual annotations droppable, so
                // checking for "drop" on annotation here,
                // and if so re-routing to add to existing annotation
                if (target_track.annot_under_mouse != null)  {
                    if (target_track.verbose_drop)  {
                        console.log("draggable dropped onto annot: ");
                        console.log(target_track.annot_under_mouse.feature);
                    }
                    target_track.addToAnnotation(target_track.annot_under_mouse.feature, dropped_feats);
                }
                else if (target_track.track_under_mouse_drag) {
                    if (target_track.verbose_drop)  { console.log("draggable dropped on AnnotTrack"); }
                    target_track.createAnnotations(dropped_feats);
                }
                // making sure annot_under_mouse is cleared
                // (should do this in the drop? but need to make sure _not_ null
                // when
                target_track.annot_under_mouse = null;
                target_track.track_under_mouse_drag = false;
            }
        } );
        if( target_track.verbose_drop) { console.log("finished making droppable target"); }
    },

    createAnnotations: function(selection_records)  {
        var target_track = this;
        var featuresToAdd = new Array();
        var parentFeatures = new Object();
        var subfeatures = [];
        var strand;
        var parentFeature;
        for (var i in selection_records)  {
            var dragfeat = selection_records[i].feature;
            var is_subfeature = !! dragfeat.parent();
            var parent = is_subfeature ? dragfeat.parent() : dragfeat;
            var parentId = parent.id();
            parentFeatures[parentId] = parent;
            
            if (strand == undefined) {
                strand = dragfeat.get("strand");
            }
            else if (strand != dragfeat.get("strand")) {
                strand = -2;
            }
            
            if (is_subfeature) {
                subfeatures.push(dragfeat);
                if (!parentFeature) {
                    parentFeature = dragfeat.parent();
                }
            }
            else {
                var children = dragfeat.get("subfeatures");
				if(!children) {
                    alert("This element cannot be annotated as a gene. Please choose a different type of annotation");
                    return;
                }
                for (var j = 0; j < children.length; ++j) {
                    subfeatures.push(children[j]);
                 }
                if (!parentFeature) {
                    parentFeature = dragfeat;
                }
            }
        }
        
        function process() {
            var keys = Object.keys(parentFeatures);
            var singleParent = keys.length == 1;
            var featureToAdd;
            if (singleParent) {
                
                featureToAdd = JSONUtils.makeSimpleFeature(parentFeatures[keys[0]]);
            }
            else {
                featureToAdd = new SimpleFeature({ data: { strand : strand } });
            }
            if(!featureToAdd.get('name')) {
                featureToAdd.set('name',featureToAdd.get('id'));
            }
            featureToAdd.set("strand", strand);
            var fmin = undefined;
            var fmax = undefined;
            featureToAdd.set('subfeatures', new Array());
            for (var i = 0; i < subfeatures.length; ++i) {
                var subfeature = subfeatures[i];
                if (!singleParent && SequenceOntologyUtils.cdsTerms[subfeature.get("type")]) {
                    continue;
                }
                var dragfeat = JSONUtils.makeSimpleFeature(subfeature);
                dragfeat.set("strand", strand);
                var childFmin = dragfeat.get('start');
                var childFmax = dragfeat.get('end');
                if (fmin === undefined || childFmin < fmin) {
                    fmin = childFmin;
                }
                if (fmax === undefined || childFmax > fmax) {
                    fmax = childFmax;
                }
                featureToAdd.get("subfeatures").push( dragfeat );
            }
            featureToAdd.set( "start", fmin );
            featureToAdd.set( "end",   fmax );
            var afeat = JSONUtils.createApolloFeature( featureToAdd, "mRNA", true );
            featuresToAdd.push(afeat);

            var postData = { "track": target_track.getUniqueTrackName(), "features": featuresToAdd, "operation": "add_transcript" };
            target_track.executeUpdateOperation(postData);

        };
        
        if (strand == -2) {
            var content = dojo.create("div");
            var message = dojo.create("div", { className: "confirm_message", innerHTML: "Creating annotation with subfeatures in opposing strands.  Choose strand:" }, content);
            var buttonsDiv = dojo.create("div", { className: "confirm_buttons" }, content);
            var plusButton = dojo.create("button", { className: "confirm_button", innerHTML: "Plus" }, buttonsDiv);
            var minusButton = dojo.create("button", { className: "confirm_button", innerHTML: "Minus" }, buttonsDiv);
            var cancelButton = dojo.create("button", { className: "confirm_button", innerHTML: "Cancel" }, buttonsDiv);
            dojo.connect(plusButton, "onclick", function() {
                strand = 1;
                target_track.closeDialog();
            });
            dojo.connect(minusButton, "onclick", function() {
                strand = -1;
                target_track.closeDialog();
            });
            dojo.connect(cancelButton, "onclick", function() {
                target_track.closeDialog();
            });
            var handle = dojo.connect(target_track.popupDialog, "onHide", function() {
                dojo.disconnect(handle);
                if (strand != -2) {
                    process();
                }
            });
            this.openDialog("Confirm", content);
            return;
        }
        else {
            process();
        }

    },
    
    createGenericAnnotations: function(feats, type, subfeatType, topLevelType) {
        var target_track = this;
        var featuresToAdd = new Array();
        var parentFeatures = new Object();
        for (var i in feats)  {
            var dragfeat = feats[i];

            var is_subfeature = !! dragfeat.parent();
            var parentId = is_subfeature ? dragfeat.parent().id() : dragfeat.id();

            if (parentFeatures[parentId] === undefined) {
                parentFeatures[parentId] = [];
                parentFeatures[parentId].isSubfeature = is_subfeature;
            }
            parentFeatures[parentId].push(dragfeat);
        }

        for (var i in parentFeatures) {
            var featArray = parentFeatures[i];
            if (featArray.isSubfeature) {
                var parentFeature = featArray[0].parent();
                var fmin = undefined;
                var fmax = undefined;
                var featureToAdd = JSONUtils.makeSimpleFeature(parentFeature);
                featureToAdd.set('subfeatures', new Array());
                for (var k = 0; k < featArray.length; ++k) {
                    // var dragfeat = featArray[k];
                    var dragfeat = JSONUtils.makeSimpleFeature(featArray[k]);
                    var childFmin = dragfeat.get('start');
                    var childFmax = dragfeat.get('end');
                    if (fmin === undefined || childFmin < fmin) {
                        fmin = childFmin;
                    }
                    if (fmax === undefined || childFmax > fmax) {
                        fmax = childFmax;
                    }
                    featureToAdd.get("subfeatures").push( dragfeat );
                }
                featureToAdd.set( "start", fmin );
                featureToAdd.set( "end",   fmax );
                var afeat = JSONUtils.createApolloFeature( featureToAdd, type, true, subfeatType );
                if (topLevelType) {
                    var topLevel = new Object();
                    topLevel.location = dojo.clone(afeat.location);
                    topLevel.type = dojo.clone(afeat.type);
                    topLevel.type.name = topLevelType;
                    topLevel.children = new Array();
                    topLevel.children.push(afeat);
                    afeat = topLevel;
                }
                featuresToAdd.push(afeat);
            }
            else {
                for (var k = 0; k < featArray.length; ++k) {
                    var dragfeat = featArray[k];
                    var afeat = JSONUtils.createApolloFeature( dragfeat, type, true, subfeatType);
                    if (topLevelType) {
                        var topLevel = new Object();
                        topLevel.location = dojo.clone(afeat.location);
                        topLevel.type = dojo.clone(afeat.type);
                        topLevel.type.name = topLevelType;
                        topLevel.children = new Array();
                        topLevel.children.push(afeat);
                        afeat = topLevel;
                    }
                    featuresToAdd.push(afeat);
                }
            }
        }
        var postData = { "track": target_track.getUniqueTrackName(), "features": featuresToAdd, "operation": "add_feature" };
        target_track.executeUpdateOperation(postData);
    },
    
    createGenericOneLevelAnnotations: function(feats, type, strandless) {
        var target_track = this;
        var featuresToAdd = new Array();
        var parentFeatures = new Object();
        for (var i in feats)  {
            var dragfeat = feats[i];

            var is_subfeature = !! dragfeat.parent();
            var parentId = is_subfeature ? dragfeat.parent().id() : dragfeat.id();

            if (parentFeatures[parentId] === undefined) {
                parentFeatures[parentId] = new Array();
                parentFeatures[parentId].isSubfeature = is_subfeature;
            }
            parentFeatures[parentId].push(dragfeat);
        }

        for (var i in parentFeatures) {
            var featArray = parentFeatures[i];
            if (featArray.isSubfeature) {
                var parentFeature = featArray[0].parent();
                var fmin = undefined;
                var fmax = undefined;
                // var featureToAdd = $.extend({}, parentFeature);
                var featureToAdd = JSONUtils.makeSimpleFeature(parentFeature);
                featureToAdd.set('subfeatures', new Array());
                for (var k = 0; k < featArray.length; ++k) {
                    // var dragfeat = featArray[k];
                    var dragfeat = JSONUtils.makeSimpleFeature(featArray[k]);
                    var childFmin = dragfeat.get('start');
                    var childFmax = dragfeat.get('end');
                    if (fmin === undefined || childFmin < fmin) {
                        fmin = childFmin;
                    }
                    if (fmax === undefined || childFmax > fmax) {
                        fmax = childFmax;
                    }
                }
                featureToAdd.set( "start", fmin );
                featureToAdd.set( "end",   fmax );
                if (strandless) {
                    featureToAdd.set( "strand", 0 );
                }
                var afeat = JSONUtils.createApolloFeature( featureToAdd, type, true );
                featuresToAdd.push(afeat);
            }
            else {
                for (var k = 0; k < featArray.length; ++k) {
                    var dragfeat = JSONUtils.makeSimpleFeature(featArray[k]);
                    var subfeat=dragfeat.get("subfeatures");
                    if(subfeat) subfeat.length=0; // clear
                                                    // subfeatures
                    if (strandless) {
                        dragfeat.set( "strand", 0 );
                    }
                    dragfeat.set("name", featArray[k].get("name"));
                    var afeat = JSONUtils.createApolloFeature( dragfeat, type, true);
                    featuresToAdd.push(afeat);
                }
            }
        }
        var postData = { "track": target_track.getUniqueTrackName(), "features":featuresToAdd, "operation": "add_feature" };
        target_track.executeUpdateOperation(postData);
    },

    duplicateSelectedFeatures: function() {
        var selected = this.selectionManager.getSelection();
        var selfeats = this.selectionManager.getSelectedFeatures();
        this.selectionManager.clearSelection();
        this.duplicateAnnotations(selfeats);
    },

    duplicateAnnotations: function(feats)  {
        var track = this;
        var featuresToAdd = new Array();
        var subfeaturesToAdd = new Array();
        var proteinCoding = false;
        for( var i in feats )  {
            var feat = feats[i];
            var is_subfeature = !! feat.parent() ;
            if (is_subfeature) {
                subfeaturesToAdd.push(feat);
            }
            else {
                featuresToAdd.push(feat);
            }
        }
        if (subfeaturesToAdd.length > 0) {
            var feature = new SimpleFeature();
            var subfeatures = new Array();
            feature.set( 'subfeatures', subfeatures );
            var fmin = undefined;
            var fmax = undefined;
            var strand = undefined;
            for (var i = 0; i < subfeaturesToAdd.length; ++i) {
                var subfeature = subfeaturesToAdd[i];
                if (fmin === undefined || subfeature.get('start') < fmin) {
                    fmin = subfeature.get('start');
                }
                if (fmax === undefined || subfeature.get('end') > fmax) {
                    fmax = subfeature.get('end');
                }
                if (strand === undefined) {
                    strand = subfeature.get('strand');
                }
                subfeatures.push(subfeature);
            }
            feature.set('start', fmin );
            feature.set('end', fmax );
            feature.set('strand', strand );
            feature.set('type', subfeaturesToAdd[0].parent().get('type'));
            feature.afeature = subfeaturesToAdd[0].parent().afeature;
            featuresToAdd.push(feature);
        }
        for (var i = 0; i < featuresToAdd.length; ++i) {
            var feature = featuresToAdd[i];
            if (this.isProteinCoding(feature)) {
                var feats = [JSONUtils.createApolloFeature( feature, "mRNA")];
                var postData = { "track":  track.getUniqueTrackName(), "features": feats, "operation": "add_transcript" };
                track.executeUpdateOperation(postData);
            }
            else if (feature.afeature.parent_id) {
                var feats = [feature];
                this.createGenericAnnotations([feature], feature.get("type"), feature.get("subfeatures")[0].get("type"), feature.afeature.parent_type.name);
            }
            else {
                if (!feature.get("name")) {
                    feature.set("name", feature.afeature.name);
                }
                var feats = [feature];
                this.createGenericOneLevelAnnotations([feature], feature.get("type"), feature.get("strand") == 0);
            }
        }
    },

    /**
     * If there are multiple AnnotTracks, each has a separate
     * FeatureSelectionManager (contrasted with DraggableFeatureTracks, which
     * all share the same selection and selection manager
     */
    deleteSelectedFeatures: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.deleteAnnotations(selected);
    },

    deleteAnnotations: function(records) {
        var track = this;
        var features = [];
        var parents = {};
        var toBeDeleted = [];
        for (var i in records)  {
            var record = records[i];
            var selfeat = record.feature;
            var uniqueName = selfeat.getUniqueName();
            var trackdiv = track.div;
            var trackName = track.getUniqueTrackName();
            if (!selfeat.parent()) {
                if (confirm("Deleting feature " + selfeat.get("name") + " cannot be undone.  Are you sure you want to delete?")) {
                    toBeDeleted.push(uniqueName);
                }
            }
            else {
                var children = parents[selfeat.parent().id()] || (parents[selfeat.parent().id()] = []);
                children.push(selfeat);
            }
        }
        for (var id in parents) {
            var children = parents[id];
            if (SequenceOntologyUtils.exonTerms[children[0].get("type")]) {
                var numExons = 0;
                var subfeatures = children[0].parent().get("subfeatures");
                for (var i = 0; i < subfeatures.length; ++i) {
                    if (SequenceOntologyUtils.exonTerms[subfeatures[i].get("type")]) {
                        ++numExons;
                    }
                }
                if (numExons == children.length) {
                    if (confirm("Deleting feature " + children[0].parent().get("name") + " cannot be undone.  Are you sure you want to delete?")) {
                        toBeDeleted.push(id);
                    }
                    continue;
                }
            }
            else if (children.length == children[0].parent().get("subfeatures").length) {
                if (confirm("Deleting feature " + children[0].parent().get("name") + " cannot be undone.  Are you sure you want to delete?")) {
                    toBeDeleted.push(id);
                }
                continue;
            }
            for (var i = 0; i < children.length; ++i) {
                toBeDeleted.push(children[i].getUniqueName());
            }
        }
        for (var i = 0; i < toBeDeleted.length; ++i) {
            features.push(  { "uniquename": toBeDeleted[i] });
        }
        if (features.length == 0) {
            return;
        }
        if (this.verbose_delete)  {
            console.log("annotations to delete:");
            console.log(features);
        }
        var postData = { "track": trackName, "features": features, "operation": "delete_feature" };
        track.executeUpdateOperation(postData);
    }, 

    mergeSelectedFeatures: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.mergeAnnotations(selected);
    },

    mergeAnnotations: function(selection) {
        var track = this;
        var annots = []; 
        for (var i=0; i<selection.length; i++)  { 
            annots[i] = selection[i].feature; 
        }

        var sortedAnnots = track.sortAnnotationsByLocation(annots);
        var leftAnnot = sortedAnnots[0];
        var rightAnnot = sortedAnnots[sortedAnnots.length - 1];
        var trackName = this.getUniqueTrackName();


        var features=[];
        var operation;
        // merge exons
        if (leftAnnot.parent() && rightAnnot.parent() && leftAnnot.parent() == rightAnnot.parent()) {
            features.push({ "uniquename": leftAnnot.id() });
            features.push({ "uniquename": rightAnnot.id() });
            operation = "merge_exons";
        }
        // merge transcripts
        else {
            var leftTranscriptId = leftAnnot.parent() ? leftAnnot.parent().id() : leftAnnot.id();
            var rightTranscriptId = rightAnnot.parent() ? rightAnnot.parent().id() : rightAnnot.id();
            features.push({ "uniquename": leftTranscriptId });
            features.push({ "uniquename": rightTranscriptId });
            operation = "merge_transcripts";
        }
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    splitSelectedFeatures: function(event)  {
        var selected = this.selectionManager.getSelectedFeatures();
        this.selectionManager.clearSelection();
        this.splitAnnotations(selected, event);
    },

    splitAnnotations: function(annots, event) {
        // can only split on max two elements
        if( annots.length > 2 ) {
            return;
        }
        var track = this;
        var sortedAnnots = track.sortAnnotationsByLocation(annots);
        var leftAnnot = sortedAnnots[0];
        var rightAnnot = sortedAnnots[sortedAnnots.length - 1];
        var trackName = track.getUniqueTrackName();

        var features=[];
        var operation;
        // split exon
        if (leftAnnot == rightAnnot) {
            var coordinate = this.getGenomeCoord(event);
            features.push({ "uniquename": leftAnnot.id(), "location": { "fmax": coordinate, "fmin": coordinate + 1 } });
            operation = "split_exon";
        }
        // split transcript
        else if (leftAnnot.parent() == rightAnnot.parent()) {
            features.push({ "uniquename":leftAnnot.id() });
            features.push({ "uniquename": rightAnnot.id() });
            operation = "split_transcript";
        }
        else {
            return;
        }
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    makeIntron: function(event)  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.makeIntronInExon(selected, event);
    },

    makeIntronInExon: function(records, event) {
        if (records.length > 1) {
            return;
        }
        var track = this;
        var annot = records[0].feature;
        var coordinate = this.getGenomeCoord(event);
        var features = [ { "uniquename": annot.id(), "location": { "fmin": coordinate } } ];
        var operation = "make_intron";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    setTranslationStart: function(event, setStart)  {
        // var selected = this.selectionManager.getSelection();
        var selfeats = this.selectionManager.getSelectedFeatures();
        this.selectionManager.clearSelection();
        this.setTranslationStartInCDS(selfeats, event);
    },

    setTranslationStartInCDS: function(annots, event) {
        if (annots.length > 1) {
            return;
        }
        var track = this;
        var annot = annots[0];
        var coordinate = this.getGenomeCoord(event);
        console.log("called setTranslationStartInCDS to: " + coordinate);

        var setStart = annot.parent() ? !annot.parent().get("manuallySetTranslationStart") : !annot.get("manuallySetTranslationStart");
        var uid = annot.parent() ? annot.parent().id() : annot.id();
        var feature={ "uniquename": uid };
        if(setStart) feature.location = { "fmin": coordinate }
        var features = [ feature ];
        var operation = "set_translation_start";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    setTranslationEnd: function(event)  {
        var selfeats = this.selectionManager.getSelectedFeatures();
        this.selectionManager.clearSelection();
        this.setTranslationEndInCDS(selfeats, event);
    },

    setTranslationEndInCDS: function(annots, event) {
        if (annots.length > 1) {
            return;
        }
        var track = this;
        var annot = annots[0];
        var coordinate = this.getGenomeCoord(event);
        console.log("called setTranslationEndInCDS to: " + coordinate);

        var setEnd = annot.parent() ? !annot.parent().get("manuallySetTranslationEnd") : !annot.get("manuallySetTranslationEnd");
        var uid = annot.parent() ? annot.parent().id() : annot.id();
        var feature={ "uniquename": uid };
        if(setStart) feature.location = { "fmax": coordinate };
        var features = [ feature ];
        var operation = "set_translation_end";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    flipStrand: function()  {
        var selected = this.selectionManager.getSelection();
        this.flipStrandForSelectedFeatures(selected);
    },

    flipStrandForSelectedFeatures: function(records) {
        var track = this;
        var uniqueNames = new Object();
        for (var i in records)  {
        var record = records[i];
        var selfeat = record.feature;
        var seltrack = record.track;
            var topfeat = this.getTopLevelAnnotation(selfeat);
            var uniqueName = topfeat.id();
            // just checking to ensure that all features in selection are from
            // this track
            if (seltrack === track)  {
                uniqueNames[uniqueName] = 1;
            }
        }
        var features = [];
        for (var uniqueName in uniqueNames) {
            features.push({ "uniquename": uniqueName });
        }
        var operation = "flip_strand";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    setLongestORF: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.setLongestORFForSelectedFeatures(selected);
    },

    setLongestORFForSelectedFeatures: function(selection) {
        var track = this;
        var features = [];
        for (var i in selection)  {
            var annot = this.getTopLevelAnnotation(selection[i].feature);
        var atrack = selection[i].track;
            var uniqueName = annot.id();
            // just checking to ensure that all features in selection are from
            // this track
            if (atrack === track)  {
                var trackdiv = track.div;
                var trackName = track.getUniqueTrackName();

                features.push({ "uniquename": uniqueName });
            }
        }
        var operation = "set_longest_orf";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    setReadthroughStopCodon: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.setReadthroughStopCodonForSelectedFeatures(selected);
    },

    setReadthroughStopCodonForSelectedFeatures: function(selection) {
        var track = this;
        var features = [];
        for (var i in selection)  {
            var annot = this.getTopLevelAnnotation(selection[i].feature);
            var atrack = selection[i].track;
            var uniqueName = annot.id();
            if (atrack === track)  {
                var trackdiv = track.div;
                var trackName = track.getUniqueTrackName();

                features.push({ "uniquename": uniqueName, "readthrough_stop_codon": !annot.data.readThroughStopCodon });
            }
        }
        var operation = "set_readthrough_stop_codon";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },
    
    setAsFivePrimeEnd: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.webapollo.featSelectionManager.clearSelection();
        this.setAsFivePrimeEndForSelectedFeatures(selectedAnnots, selectedEvidence);
    },

    setAsFivePrimeEndForSelectedFeatures: function(selectedAnnots, selectedEvidence) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var evidence = selectedEvidence[0].feature;
        var uniqueName = annot.id();
        var fmin, fmax;
        if (annot.get("strand") == -1) {
            fmin = annot.get("start");
            fmax = evidence.get("end");
        }
        else {
            fmin = evidence.get("start");
            fmax = annot.get("end");
        }
        var features = [ { "uniquename": uniqueName , "location": { "fmin": fmin, "fmax": fmax} } ];
        var operation = "set_exon_boundaries";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        track.executeUpdateOperation(postData);
    },

    setAsThreePrimeEnd: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.webapollo.featSelectionManager.clearSelection();
        this.setAsThreePrimeEndForSelectedFeatures(selectedAnnots, selectedEvidence);
    },

    setAsThreePrimeEndForSelectedFeatures: function(selectedAnnots, selectedEvidence) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var evidence = selectedEvidence[0].feature;
        var uniqueName = annot.id();
        var fmin, fmax;
        if (annot.get("strand") == -1) {
            fmin = evidence.get("start");
            fmax = annot.get("end");
        }
        else {
            fmin = annot.get("start");
            fmax = evidence.get("end");
        }
        var features = [ { "uniquename": uniqueName, "location": { "fmin": fmin, "fmax": fmax } } ];
        var operation = "set_exon_boundaries";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },
    
    setBothEnds: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.webapollo.featSelectionManager.clearSelection();
        this.setBothEndsForSelectedFeatures(selectedAnnots, selectedEvidence);
    },

    setBothEndsForSelectedFeatures: function(selectedAnnots, selectedEvidence) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var evidence = selectedEvidence[0].feature;
        var uniqueName = annot.id();
        var fmin, fmax;
        if (annot.get("strand") == -1) {
            fmin = evidence.get("start");
            fmax = evidence.get("end");
        }
        else {
            fmin = evidence.get("start");
            fmax = evidence.get("end");
        }
        var features = [ { "uniquename": uniqueName, "location": { "fmin": fmin, "fmax": fmax } } ];
        var operation = "set_exon_boundaries";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },
    
    setToDownstreamDonor: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.setToDownstreamDonorForSelectedFeatures(selectedAnnots);
    },

    setToDownstreamDonorForSelectedFeatures: function(selectedAnnots) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var uniqueName = annot.id();
        var features = [ { "uniquename": uniqueName } ];
        var operation = "set_to_downstream_donor";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },

    setToUpstreamDonor: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.setToUpstreamDonorForSelectedFeatures(selectedAnnots);
    },

    setToUpstreamDonorForSelectedFeatures: function(selectedAnnots) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var uniqueName = annot.id();
        var features = [ { "uniquename": uniqueName } ];
        var operation = "set_to_upstream_donor";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },

    setToDownstreamAcceptor: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.setToDownstreamAcceptorForSelectedFeatures(selectedAnnots);
    },

    setToDownstreamAcceptorForSelectedFeatures: function(selectedAnnots) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var uniqueName = annot.id();
        var features = [ { "uniquename": uniqueName } ];
        var operation = "set_to_downstream_acceptor";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },

    setToUpstreamAcceptor: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.setToUpstreamAcceptorForSelectedFeatures(selectedAnnots);
    },

    setToUpstreamAcceptorForSelectedFeatures: function(selectedAnnots) {
        var track = this;
        var annot = selectedAnnots[0].feature;
        var uniqueName = annot.id();
        var features = [ { "uniquename": uniqueName } ];
        var operation = "set_to_upstream_acceptor";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },

    lockAnnotation: function() {
        var selectedAnnots = this.selectionManager.getSelection();
        this.selectionManager.clearAllSelection();
        this.lockAnnotationForSelectedFeatures(selectedAnnots);
    },
    
    lockAnnotationForSelectedFeatures: function(selectedAnnots) {
        var track = this;
        var annot = this.getTopLevelAnnotation(selectedAnnots[0].feature);
        var uniqueName = annot.id();
        var features = [ { "uniquename":  uniqueName } ];
        var operation = annot.get("locked") ? "unlock_feature" : "lock_feature";
        var trackName = track.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation};
        track.executeUpdateOperation(postData);
    },
    undo: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.undoSelectedFeatures(selected);
    },

    undoSelectedFeatures: function(records) {
        var track = this;
        var uniqueNames = [];
        for (var i in records)  {
            var record = records[i];
            var selfeat = record.feature;
            var seltrack = record.track;
            var topfeat = this.getTopLevelAnnotation(selfeat);
            var uniqueName = topfeat.id();
            // just checking to ensure that all features in selection are from
            // this track
            if (seltrack === track)  {
                var trackdiv = track.div;
                var trackName = track.getUniqueTrackName();
                uniqueNames.push(uniqueName);
            }
        }
        this.undoFeaturesByUniqueName(uniqueNames, 1);
    },

    undoFeaturesByUniqueName: function(uniqueNames, count) {
        var track = this;
        var features = [];
        for (var i = 0; i < uniqueNames.length; ++i) {
            features.push({ "uniquename": uniqueNames[i] });
        }
        var operation = "undo";
        var trackName = this.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        if(count) postData.count=count;
        this.executeUpdateOperation(postData, function(response) {
            if (response && response.confirm) {
                if (track.handleConfirm(response.confirm)) {
                    postData = { "track": trackName, "features": features, "operation": operation, "confirm": true };
                    track.executeUpdateOperation(postData);
                }
            }
        });
    },
    
    redo: function()  {
        var selected = this.selectionManager.getSelection();
        this.selectionManager.clearSelection();
        this.redoSelectedFeatures(selected);
    },

    redoSelectedFeatures: function(records) {
        var track = this;
        var uniqueNames = [];
        for (var i in records)  {
            var record = records[i];
            var selfeat = record.feature;
            var seltrack = record.track;
            var topfeat = this.getTopLevelAnnotation(selfeat);
            var uniqueName = topfeat.id();
            // just checking to ensure that all features in selection are from
            // this track
            if (seltrack === track)  {
                uniqueNames.push(uniqueName);
            }
        }
        this.redoFeaturesByUniqueName(uniqueNames, 1);
    },
    
    redoFeaturesByUniqueName: function(uniqueNames, count) {
        var features = [];
        for (var i = 0; i < uniqueNames.length; ++i) {
            features.push({ "uniquename": uniqueNames[i] });
        }
        var operation = "redo";
        var trackName = this.getUniqueTrackName();
        var postData = { "track": trackName, "features": features, "operation": operation };
        if(count) postData.count=count;
        this.executeUpdateOperation(postData);
    },
    

    getGff3: function()  {
        var selected = this.selectionManager.getSelection();
        this.getGff3ForSelectedFeatures(selected);
    },

    getGff3ForSelectedFeatures: function(records) {
        var track = this;

        var content = dojo.create("div", { className: "get_gff3" });
        var textArea = dojo.create("textarea", { className: "gff3_area", readonly: true }, content);

        var fetchGff3 = function() {
            var features = [];
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

                    features.push({ "uniquename": uniqueName });
                }
            }
            var operation = "get_gff3";
            var trackName = track.getUniqueTrackName();
            var postData = { "track": trackName, "features": features, "operation": operation };
            dojo.xhrPost( {
                postData: JSON.stringify(postData),
                url: context_path + "/AnnotationEditorService",
                handleAs: "text",
                timeout: 5000 * 1000, // Time in milliseconds
                load: function(response, ioArgs) {
                    var textAreaContent = response;
                    dojo.attr(textArea, "innerHTML", textAreaContent);
                },
                // The ERROR function will be called in an error case.
                error: function(response, ioArgs) {
                    track.handleError(response);
                    console.log(response);
                    console.log("Annotation server error--maybe you forgot to login to the server?");
                    console.error("HTTP status code: ", ioArgs.xhr.status);
                    return response;
                }

            });
        };
        fetchGff3(records);

        this.openDialog("GFF3", content);
    },
    searchSequence: function() {
        var track = this;
        var starts = new Object();
        var browser = track.gview.browser;
        for (i in browser.allRefs) {
            var refSeq = browser.allRefs[i];
            starts[refSeq.name] = refSeq.start;
        }
        var search = new SequenceSearch(context_path);
        search.setRedirectCallback(function(id, fmin, fmax) {
            var loc = id + ":" + fmin + "-" + fmax;
            var locobj = {
                    ref: id, 
                    start: fmin, 
                    end: fmax
            };
            if (id == track.refSeq.name) {
                var highlightSearchedRegions = track.gview.browser.config.highlightSearchedRegions;
                browser.config.highlightSearchedRegions = true;
                browser.showRegionWithHighlight(locobj);
                browser.config.highlightSearchedRegions = highlightSearchedRegions;
            }
            else {
                var highlightSearchedRegions = track.gview.browser.config.highlightSearchedRegions;
                browser.config.highlightSearchedRegions = true;
                browser.showRegionWithHighlight(locobj);
                browser.config.highlightSearchedRegions = highlightSearchedRegions;
            }
        });
        search.setErrorCallback(function(response) {
            track.handleError(response);
        });
        var content = search.searchSequence(track.getUniqueTrackName(), track.refSeq.name, starts);
        if (content) {
            this.openDialog("Search sequence", content);
        }
    }, 

    exportData: function(key, options) {
        var track = this;
        var adapter = key;
        var content = dojo.create("div");
        var waitingDiv = dojo.create("div", { innerHTML: "<img class='waiting_image' src='plugins/WebApollo/img/loading.gif' />" }, content);
        var responseDiv = dojo.create("div", { className: "export_response" }, content);

        dojo.xhrGet( {
		url: context_path + "/IOService?operation=write&adapter=" + adapter + "&tracks=" + track.getUniqueTrackName() + "&" + options,
		handleAs: "text",
		load: function(response, ioArgs) {
		    console.log("/IOService returned, called load()");
		    dojo.style(waitingDiv, { display: "none" } );
		    response = response.replace("href='", "href='../");

                /*
                 * var iframeDoc = responseIFrame.contentWindow.document;
                 * iframeDoc.open(); iframeDoc.write(response); iframeDoc.close();
                 */
                responseDiv.innerHTML = response;
            }, 
            // The ERROR function will be called in an error case.
            error: function(response, ioArgs) {
                dojo.style(waitingDiv, { display: "none" } );
                responseDiv.innerHTML = "Unable to export data";
                track.handleError(response);
            }
        });
        track.openDialog("Export " + key, content);
    }, 


    scrollToNextEdge: function(event)  {
        // var coordinate = this.getGenomeCoord(event);
        var track = this;
        var vregion = this.gview.visibleRegion();
        var coordinate = (vregion.start + vregion.end)/2;
        var selected = this.selectionManager.getSelection();
        if (selected && (selected.length > 0)) {
            
            function centerAtBase(position) {
                track.gview.centerAtBase(position, false);
                track.selectionManager.removeFromSelection(selected[0]);
                var subfeats = selfeat.get("subfeatures");
                for (var i = 0; i < subfeats.length; ++i) {
                    if (track.selectionManager.unselectableTypes[subfeats[i].get("type")]) {
                        continue;
                    }
                    // skip CDS features
                    if (SequenceOntologyUtils.cdsTerms[subfeats[i].get("type")] || subfeats[i].get("type") == "wholeCDS") {
                        continue;
                    }
                    if (position >= subfeats[i].get("start") && position <= subfeats[i].get("end")) {
                        track.selectionManager.addToSelection( { feature: subfeats[i], track: track } );
                        break;
                    }
                }
            };
            
            var selfeat = selected[0].feature;
            // find current center genome coord, compare to subfeatures,
            // figure out nearest subfeature right of center of view
            // if subfeature overlaps, go to right edge
            // else go to left edge
            // if to left, move to left edge
            // if to right,
            while (selfeat.parent()) {
                selfeat = selfeat.parent();
            }
            // only support scrolling if the feature isn't fully visible
            if (vregion.start <= selfeat.get("start") && vregion.end >= selfeat.get("end")) {
                return;
            }
            var coordDelta = Number.MAX_VALUE;
            var pmin = selfeat.get('start');
            var pmax = selfeat.get('end');
            if ((coordinate - pmax) > 10) {
                centerAtBase(pmin);
            }
            else  {
                var childfeats = selfeat.children();                
                for (var i=0; i<childfeats.length; i++)  {
                    var cfeat = childfeats[i];
                    var cmin = cfeat.get('start');
                    var cmax = cfeat.get('end');
                    // if (cmin > coordinate) {
                    if ((cmin - coordinate) > 10) { // fuzz factor of 10 bases
                        coordDelta = Math.min(coordDelta, cmin-coordinate);
                    }
                    // if (cmax > coordinate) {
                    if ((cmax - coordinate) > 10) { // fuzz factor of 10 bases
                        coordDelta = Math.min(coordDelta, cmax-coordinate);
                    }
                }
                // find closest edge right of current coord
                if (coordDelta != Number.MAX_VALUE)  {
                    var newCenter = coordinate + coordDelta;
                    centerAtBase(newCenter);
                }
            }
        }
    }, 

    scrollToPreviousEdge: function(event) {
        // var coordinate = this.getGenomeCoord(event);
        var track = this;
        var vregion = this.gview.visibleRegion();
        var coordinate = (vregion.start + vregion.end)/2;
        var selected = this.selectionManager.getSelection();
        if (selected && (selected.length > 0)) {
            function centerAtBase(position) {
                track.gview.centerAtBase(position, false);
                track.selectionManager.removeFromSelection(selected[0]);
                var subfeats = selfeat.get("subfeatures");
                for (var i = 0; i < subfeats.length; ++i) {
                    if (track.selectionManager.unselectableTypes[subfeats[i].get("type")]) {
                        continue;
                    }
                    // skip CDS features
                    if (SequenceOntologyUtils.cdsTerms[subfeats[i].get("type")] || subfeats[i].get("type") == "wholeCDS") {
                        continue;
                    }
                    if (position >= subfeats[i].get("start") && position <= subfeats[i].get("end")) {
                        track.selectionManager.addToSelection( { feature: subfeats[i], track: track } );
                        break;
                    }
                }
            };
            
            var selfeat = selected[0].feature;
            // find current center genome coord, compare to subfeatures,
            // figure out nearest subfeature right of center of view
            // if subfeature overlaps, go to right edge
            // else go to left edge
            // if to left, move to left edge
            // if to right,
            while (selfeat.parent()) {
                selfeat = selfeat.parent();
            }
            // only support scrolling if the feature isn't fully visible
            if (vregion.start <= selfeat.get("start") && vregion.end >= selfeat.get("end")) {
                return;
            }
            var coordDelta = Number.MAX_VALUE;
            var pmin = selfeat.get('start');
            var pmax = selfeat.get('end');
            if ((pmin - coordinate) > 10) {
                centerAtBase(pmax);
            }
            else  {
                var childfeats = selfeat.children();                
                for (var i=0; i<childfeats.length; i++)  {
                    var cfeat = childfeats[i];
                    var cmin = cfeat.get('start');
                    var cmax = cfeat.get('end');
                    // if (cmin > coordinate) {
                    if ((coordinate - cmin) > 10) { // fuzz factor of 10 bases
                        coordDelta = Math.min(coordDelta, coordinate-cmin);
                    }
                    // if (cmax > coordinate) {
                    if ((coordinate - cmax) > 10) { // fuzz factor of 10 bases
                        coordDelta = Math.min(coordDelta, coordinate-cmax);
                    }
                }
                // find closest edge right of current coord
                if (coordDelta != Number.MAX_VALUE)  {
                    var newCenter = coordinate - coordDelta;
                    centerAtBase(newCenter);
                }
            }
        }
    }, 

    scrollToNextTopLevelFeature: function() {
        var selected = this.selectionManager.getSelection();
        if (!selected  || !selected.length) {
            return;
        }
        var features = [];
        for (var i in this.store.features) {
            features.push(this.store.features[i]);
        }
        this.sortAnnotationsByLocation(features);
        var idx = this.binarySearch(features, this.getTopLevelAnnotation(selected[0].feature));
        if (idx < 0 || idx >= features.length - 1) {
            return;
        }
        this.gview.centerAtBase(features[idx + 1].get("start"));
        this.selectionManager.removeFromSelection({ feature: selected[0].feature, track: this });
        this.selectionManager.addToSelection({ feature: features[idx + 1], track: this });
    },

    scrollToPreviousTopLevelFeature: function() {
        var selected = this.selectionManager.getSelection();
        if (!selected  || !selected.length) {
            return;
        }
        var features = [];
        for (var i in this.store.features) {
            features.push(this.store.features[i]);
        }
        this.sortAnnotationsByLocation(features);
        var idx = this.binarySearch(features, this.getTopLevelAnnotation(selected[0].feature));
        if (idx <= 0 || idx > features.length - 1) {
            return;
        }
        this.gview.centerAtBase(features[idx - 1].get("end"));
        this.selectionManager.removeFromSelection({ feature: selected[0].feature, track: this });
        this.selectionManager.addToSelection({ feature: features[idx - 1], track: this });
    },
    
    binarySearch: function(features, feature) {
        var from = 0;
        var to = features.length - 1;
        while (from <= to) {
            var mid = from + ((to - from) >> 1);
            if (feature.get("start") == features[mid].get("start") && feature.get("end") == features[mid].get("end") && feature.id() == features[mid].id()) {
                return mid;
            }
            if (feature.get("start") == features[mid].get("start")) {
                if (feature.get("end") < features[mid].get("end")) {
                    to = mid - 1;
                }
                else if (feature.get("end") > features[mid].get("end")) {
                    from = mid + 1;
                }
                else {
                    if (feature.id() < features[mid].id()) {
                        to = mid - 1;
                    }
                    else {
                        from = mid + 1;
                    }
                }
            }
            else if (feature.get("start") < features[mid].get("start")) {
                to = mid - 1;
            }
            else {
                from = mid + 1;
            }
        }
        return -1;
    },
    
    handleError: function(response) {
        console.log("ERROR: ");
        console.log(response);  // in Firebug, allows retrieval of stack trace,
                                // jump to code, etc.
        console.log(response.stack);
        //avoid eval of html content
        if(response.responseText.match("^<")!="<") {
            
            var error = JSON.parse(response.responseText);
            if (error && error.error) {
                alert(error.error);
            }
            return false;
        }
    },

    handleConfirm: function(response) {
            return confirm(response); 
    },
    
    logout: function() {
        dojo.xhrPost( {
            url: context_path + "/Login?operation=logout",
            handleAs: "json",
            timeout: 5 * 1000, // Time in milliseconds
        });
    },
    
    login: function() {
        var track = this;
        dojo.xhrGet( {
            url: context_path + "/Login",
            handleAs: "text",
            timeout: 5 * 60,
            load: function(response, ioArgs) {
                var dialog = new dojoxDialogSimple({
                    preventCache: true,
                    refreshOnShow: true,
                    executeScripts: true
                });
                if (track.config.disableJBrowseMode) {
                    dialog.hide = function() { };
                }
                dialog.startup();
                dialog.set("title", "Login");
                dialog.set("content", response);
                dialog.show();
            }
        });
    },
    
    initLoginMenu: function() {
        var track = this;
        var browser = this.gview.browser;
        if (this.permission)  {   // permission only set if permission request
                                    // succeeded
            browser.addGlobalMenuItem( 'user',
                new dijitMenuItem({
                    label: 'Logout',
                    onClick: function()  { 
                        console.log("clicked stub for logging out");
                        dojo.xhrPost( {
                            url: context_path + "/Login?operation=logout",
                            handleAs: "json",
                            timeout: 5 * 1000,
                            load: function(response, ioArgs) { //
                            },
                            error: function(response, ioArgs) { //
                            // track.handleError(response);
                            }
                        });
                    }
                })
            );
            var userMenu = browser.makeGlobalMenu('user');
            loginButton = new dijitDropDownButton(
                { className: 'user',
                  innerHTML: '<span class="usericon"></span>' + this.username,
                  title: 'user logged in: UserName',
                  dropDown: userMenu
                });
        }
        else  { 
            loginButton = new dijitButton(
                { className: 'login',
                  innerHTML: "Login",
                  onClick: function()  {
                      dojo.xhrGet( {
                          url: context_path + "/Login?operation=login",
                          handleAs: "text",
                          timeout: 5 * 60,
                          load: function(response, ioArgs) {
                              track.openDialog("Login", response);
                          }
                      });
                  }
                });
        }
        browser.afterMilestone( 'initView', function() {
            // must append after menubar is created, plugin constructor called
            // before menubar exists,
            // browser.initView called after menubar exists
            browser.menuBar.appendChild( loginButton.domNode );
        });
    }, 
    
    initAnnotContextMenu: function() {
        var thisB = this;
        contextMenuItems = new Array();
        annot_context_menu = new dijit.Menu({});
        var permission = thisB.permission;
        var index = 0;
        annot_context_menu.addChild(new dijit.MenuItem( {
            label: "Get sequence",
            onClick: function(event) {
                thisB.getSequence();
            }
        } ));
        contextMenuItems["get_sequence"] = index++;

        annot_context_menu.addChild(new dijit.MenuItem( {
            label: "Get gff3",
            onClick: function(event) {
                thisB.getGff3();
            }
        } ));
        contextMenuItems["get_gff3"] = index++;

        annot_context_menu.addChild(new dijit.MenuItem( {
            label: "Zoom to base level",
            onClick: function(event) {
                if (thisB.getMenuItem("zoom_to_base_level").get("label") == "Zoom to base level") {
                    thisB.zoomToBaseLevel(thisB.annot_context_mousedown);
                }
                else {
                    thisB.zoomBackOut(thisB.annot_context_mousedown);
                }
            }
        } ));
        contextMenuItems["zoom_to_base_level"] = index++;
        if (!(permission & Permission.WRITE)) {
            annot_context_menu.addChild(new dijit.MenuSeparator());
            index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Information Viewer (alt-click)",
                onClick: function(event) {
                    thisB.getAnnotationInfoEditor();
                }
            } ));
            contextMenuItems["annotation_info_editor"] = index++;
        }
        if (permission & Permission.WRITE) {
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Edit Annotation (alt-click)",
                onClick: function(event) {
                    thisB.getAnnotationInfoEditor();
                }
            } ));
            contextMenuItems["annotation_info_editor"] = index++;
            annot_context_menu.addChild(new dijit.MenuSeparator());
            index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Delete",
                onClick: function() {
                    thisB.deleteSelectedFeatures();
                }
            } ));
            contextMenuItems["delete"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Merge",
                onClick: function() {
                    thisB.mergeSelectedFeatures();
                }
            } ));
            contextMenuItems["merge"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Split",
                onClick: function(event) {
                    thisB.splitSelectedFeatures(thisB.annot_context_mousedown);
                }
            } ));
            contextMenuItems["split"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Duplicate",
                onClick: function(event) {
                    // use annot_context_mousedown instead of current event, since
                    // want to split
                    // at mouse position of event that triggered annot_context_menu
                    // popup
                    thisB.duplicateSelectedFeatures(thisB.annot_context_mousedown);
                }
            } ));
            contextMenuItems["duplicate"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Make intron",
                // use annot_context_mousedown instead of current event, since want
                // to split
                // at mouse position of event that triggered annot_context_menu
                // popup
                onClick: function(event) {
                    thisB.makeIntron(thisB.annot_context_mousedown);
                }
            } ));
            contextMenuItems["make_intron"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Move to opposite strand",
                onClick: function(event) {
                    thisB.flipStrand();
                }
            } ));
            contextMenuItems["flip_strand"] = index++;
            annot_context_menu.addChild(new dijit.MenuSeparator());
            index++;

            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set translation start",
                // use annot_context_mousedown instead of current event, since want
                // to split
                // at mouse position of event that triggered annot_context_menu
                // popup
                onClick: function(event) {
                    thisB.setTranslationStart(thisB.annot_context_mousedown);
                }
            } ));
            contextMenuItems["set_translation_start"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set translation end",
                // use annot_context_mousedown instead of current event, since want
                // to split
                // at mouse position of event that triggered annot_context_menu
                // popup
                onClick: function(event) {
                    thisB.setTranslationEnd(thisB.annot_context_mousedown);
                }
            } ));
            contextMenuItems["set_translation_end"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set longest ORF",
                // use annot_context_mousedown instead of current event, since want
                // to split
                // at mouse position of event that triggered annot_context_menu
                // popup
                onClick: function(event) {
                    thisB.setLongestORF();
                }
            } ));
            contextMenuItems["set_longest_orf"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set readthrough stop codon",
                onClick: function(event) {
                    thisB.setReadthroughStopCodon();
                }
            } ));
            contextMenuItems["set_readthrough_stop_codon"] = index++;
            annot_context_menu.addChild(new dijit.MenuSeparator());
            index++;
            
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set as 5' end",
                onClick: function(event) {
                    thisB.setAsFivePrimeEnd();
                }
            } ));
            contextMenuItems["set_as_five_prime_end"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set as 3' end",
                onClick: function(event) {
                    thisB.setAsThreePrimeEnd();
                }
            } ));
            contextMenuItems["set_as_three_prime_end"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Set both ends",
                onClick: function(event) {
                    thisB.setBothEnds();
                }
            } ));
            contextMenuItems["set_both_ends"] = index++;
            annot_context_menu.addChild(new dijit.MenuSeparator());
            index++;
            contextMenuItems["set_downstream_donor"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                    label: "Set to downstream splice donor",
                    onClick: function(event) {
                            thisB.setToDownstreamDonor();
                    }
            }));
            contextMenuItems["set_upstream_donor"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                    label: "Set to upstream splice donor",
                    onClick: function(event) {
                            thisB.setToUpstreamDonor();
                    }
            }));
            contextMenuItems["set_downstream_acceptor"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                    label: "Set to downstream splice acceptor",
                    onClick: function(event) {
                            thisB.setToDownstreamAcceptor();
                    }
            }));
            contextMenuItems["set_upstream_acceptor"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                    label: "Set to upstream splice acceptor",
                    onClick: function(event) {
                            thisB.setToUpstreamAcceptor();
                    }
            }));
            annot_context_menu.addChild(new dijit.MenuSeparator());
            index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Undo",
                onClick: function(event) {
                    thisB.undo();
                }
            } ));
            contextMenuItems["undo"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "Redo",
                onClick: function(event) {
                    thisB.redo();
                }
            } ));
            contextMenuItems["redo"] = index++;
            annot_context_menu.addChild(new dijit.MenuItem( {
                label: "History",
                onClick: function(event) {
                    thisB.getHistory();
                }
            } ));
            contextMenuItems["history"] = index++;
        }

        annot_context_menu.onOpen = function(event) {
            // keeping track of mousedown event that triggered annot_context_menu
            // popup,
            // because need mouse position of that event for some actions
            thisB.annot_context_mousedown = thisB.last_mousedown_event;
            if (thisB.permission & Permission.WRITE) {
                thisB.updateMenu();
            }
            dojo.forEach(this.getChildren(), function(item, idx, arr) {
                if (item instanceof dijit.MenuItem) {
                    item._setSelected(false);
                    // check for _onUnhover, since latest
                    // dijit.MenuItem does not have _onUnhover()
                    // method
                    if (item._onUnhover) { item._onUnhover(); }
                }
            });
        };
        
        annot_context_menu.startup();
    }, 


    /**
     * Add AnnotTrack data save option to track label pulldown menu Trying to make
     * it a replacement for default JBrowse data save option from ExportMixin
     * (turned off JBrowse default via config.noExport = true)
     */
    initSaveMenu: function()  {
        var track = this;
        dojo.xhrPost( {
            sync: true,
            postData: JSON.stringify({ "track": track.getUniqueTrackName(), "operation": "get_data_adapters" }),
            url: context_path + "/AnnotationEditorService",
            handleAs: "json",
            timeout: 5 * 1000, // Time in milliseconds
            // The LOAD function will be called on a successful response.
            load: function(response, ioArgs) { //
                var dataAdapters = response.data_adapters;
                for (var i = 0; i < dataAdapters.length; ++i) {
                    var dataAdapter = dataAdapters[i];
                    if (track.permission & dataAdapter.permission) {
                        track.exportAdapters.push( dataAdapter );
                    }
                }
                // remake track label pulldown menu so will include
                // dataAdapter submenu
                track.makeTrackMenu();
            },
            error: function(response, ioArgs) { //
                // track.handleError(response);
            }
        });
    }, 

    makeTrackMenu: function()  {
        this.inherited( arguments );
        var track = this;
        var options = this._trackMenuOptions();
        if( options && options.length && this.label && this.labelMenuButton && this.exportAdapters.length > 0) {
            var dataAdaptersMenu = new dijit.Menu();
            for (var i=0; i<this.exportAdapters.length; i++) {
                var dataAdapter = this.exportAdapters[i];
                if (dataAdapter.data_adapters) {
                    var submenu = new dijit.Menu({
                        label: dataAdapter.key
                    });
                    dataAdaptersMenu.addChild(new dijit.PopupMenuItem({
                        label: dataAdapter.key,
                        popup: submenu
                    }));
                    for (var j = 0; j < dataAdapter.data_adapters.length; ++j) {
                        var subAdapter = dataAdapter.data_adapters[j];
                        submenu.addChild(new dijit.MenuItem( {
                            label: subAdapter.key,
                            onClick: function(key, options) {
                                return function() {
                                    track.exportData(key, options);
                                };
                            }(subAdapter.key, subAdapter.options)
                        } ) );
                    }
                }
                else {
                    dataAdaptersMenu.addChild(new dijit.MenuItem( {
                        label: dataAdapter.key,
                        onClick: function(key, options) {
                            return function() {
                                track.exportData(key, options);
                            };
                        }(dataAdapter.key, dataAdapter.options)
                    } ) );
                }
            }
            // if there's a menu separator, add right before first seperator (which
            // is where default save is added),
            // otherwise add at end
            var mitems = this.trackMenu.getChildren();
            for (var mindex=0; mindex < mitems.length; mindex++) {
                if (mitems[mindex].type == "dijit/MenuSeparator")  { break; }
            }
             
            var savePopup = new dijit.PopupMenuItem({
                    label: "Save track data",
                    iconClass: 'dijitIconSave',
                    popup: dataAdaptersMenu });
            this.trackMenu.addChild(savePopup, mindex);
        }
    },

    getPermission: function( ) {
        var thisObj = this;
        return xhr.post(context_path + "/AnnotationEditorService", {
            data: JSON.stringify({ "track": thisObj.getUniqueTrackName(), "operation": "get_user_permission" }),
            handleAs: "json",
            timeout: 5 * 1000, // Time in milliseconds
        }).then(function(response) {
            // The LOAD function will be called on a successful response.
            var permission = response.permission;
            thisObj.permission = permission;
            var username = response.username;
            thisObj.username = username;
        });
    },

    initPopupDialog: function() {
        if (this.popupDialog) {
            return;
        }
        var track = this;
        var id = "popup_dialog";

        // deregister widget (needed if changing refseq without reloading page)
        var widget = dijit.registry.byId(id);
        if (widget) {
            widget.destroy();
        }
        this.popupDialog = new dojoxDialogSimple({
            preventCache: true,
            refreshOnShow: true,
            executeScripts: true,
            id: id
        });

        this.popupDialog.startup();
    },

    getUniqueTrackName: function() {
        return this.name + "-" + this.refSeq.name;
    },

    openDialog: function(title, data, width, height) {
        this.popupDialog.set("title", title);
        this.popupDialog.set("content", data);
        this.popupDialog.set("style", "width:" + (width ? width : "auto") + ";height:" + (height ? height : "auto"));
        this.popupDialog.show();
    },

    closeDialog: function() {
        this.popupDialog.hide();
    },

    updateMenu: function() {
        this.updateDeleteMenuItem();
        this.updateSetTranslationStartMenuItem();
        this.updateSetTranslationEndMenuItem();
        this.updateSetLongestOrfMenuItem();
        this.updateSetReadthroughStopCodonMenuItem();
        this.updateMergeMenuItem();
        this.updateSplitMenuItem();
        this.updateMakeIntronMenuItem();
        this.updateFlipStrandMenuItem();
        this.updateAnnotationInfoEditorMenuItem();
        this.updateUndoMenuItem();
        this.updateRedoMenuItem();
        this.updateZoomToBaseLevelMenuItem();
        this.updateDuplicateMenuItem();
        this.updateHistoryMenuItem();
        this.updateSetAsFivePrimeEndMenuItem();
        this.updateSetAsThreePrimeEndMenuItem();
        this.updateSetBothEndsMenuItem();
        this.updateSetNextDonorMenuItem();
        this.updateSetPreviousDonorMenuItem();
        this.updateSetNextAcceptorMenuItem();
        this.updateSetPreviousAcceptorMenuItem();
//        this.updateLockAnnotationMenuItem();
    },

    updateDeleteMenuItem: function() {
        var menuItem = this.getMenuItem("delete");
        var selected = this.selectionManager.getSelection();
        for (var i = 0; i < selected.length; ++i) {
            if (!this.canEdit(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateSetTranslationStartMenuItem: function() {
        var menuItem = this.getMenuItem("set_translation_start");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        for (var i = 0; i < selected.length; ++i) {
            if (!this.isProteinCoding(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
        var selectedFeat = selected[0].feature;
        if (selectedFeat.parent()) {
            selectedFeat = selectedFeat.parent();
        }
        if (!this.canEdit(selectedFeat)) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedFeat.get('manuallySetTranslationStart')) {
            menuItem.set("label", "Unset translation start");
        }
        else {
            menuItem.set("label", "Set translation start");
        }
    },

    updateSetTranslationEndMenuItem: function() {
        var menuItem = this.getMenuItem("set_translation_end");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        for (var i = 0; i < selected.length; ++i) {
            if (!this.isProteinCoding(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
        var selectedFeat = selected[0].feature;
        if (selectedFeat.parent()) {
            selectedFeat = selectedFeat.parent();
        }
        if (!this.canEdit(selectedFeat)) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedFeat.get('manuallySetTranslationEnd')) {
            menuItem.set("label", "Unset translation end");
        }
        else {
            menuItem.set("label", "Set translation end");
        }
    },

    updateSetLongestOrfMenuItem: function() {
        var menuItem = this.getMenuItem("set_longest_orf");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        for (var i = 0; i < selected.length; ++i) {
            if (!this.isProteinCoding(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
            if (!this.canEdit(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }

        menuItem.set("disabled", false);
    },

    updateSetReadthroughStopCodonMenuItem: function() {
        var menuItem = this.getMenuItem("set_readthrough_stop_codon");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        for (var i = 0; i < selected.length; ++i) {
            if (!this.isProteinCoding(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
            if (!this.canEdit(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
        var selectedFeat = selected[0].feature;
        if (selectedFeat.parent()) {
            selectedFeat = selectedFeat.parent();
        }
        if (selectedFeat.get('readThroughStopCodon')) {
            menuItem.set("label", "Unset readthrough stop codon");
        }
        else {
            menuItem.set("label", "Set readthrough stop codon");
        }
    },

    updateMergeMenuItem: function() {
        var menuItem = this.getMenuItem("merge");
        var selected = this.selectionManager.getSelection();
        if (selected.length < 2) {
            menuItem.set("disabled", true);
            // menuItem.domNode.style.display = "none"; // direct method for
            // hiding menuitem
            // $(menuItem.domNode).hide(); // probably better method for hiding
            // menuitem
            return;
        }
        else  {
            // menuItem.domNode.style.display = ""; // direct method for
            // unhiding menuitem
            // $(menuItem.domNode).show(); // probably better method for
            // unhiding menuitem
        }
        var strand = selected[0].feature.get('strand');
        for (var i = 1; i < selected.length; ++i) {
            if (selected[i].feature.get('strand') != strand) {
                    menuItem.set("disabled", true);
                    return;
            }
            if (!this.canEdit(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateSplitMenuItem: function() {
        var menuItem = this.getMenuItem("split");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 2) {
            menuItem.set("disabled", true);
            return;
        }
        if (selected.length == 1) {
            if (!selected[0].feature.parent()) {
                menuItem.set("disabled", true);
                return;
            }
            if (!selected[0].feature.afeature.parent_id) {
                menuItem.set("disabled", true);
                return;
            }
            if (!this.canEdit(selected[0].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        var parent = selected[0].feature.parent();
        if (!parent) {
            menuItem.set("disabled", true);
            return;
        }
        for (var i = 1; i < selected.length; ++i) {
            if (selected[i].feature.parent() != parent) {
                menuItem.set("disabled", true);
                return;
            }
            if (!this.canEdit(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateMakeIntronMenuItem: function() {
        var menuItem = this.getMenuItem("make_intron");
        var selected = this.selectionManager.getSelection();
        if( selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        if (!selected[0].feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selected[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        if (SequenceOntologyUtils.neverHasExons[selected[0].feature.get("type")]) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },

    updateFlipStrandMenuItem: function() {
        var menuItem = this.getMenuItem("flip_strand");
        var selected = this.selectionManager.getSelection();
        for (var i = 0; i < selected.length; ++i) {
            if (selected[i].feature.get("strand") == 0) {
                menuItem.set("disabled", true);
                return;
            }
            if (!this.canEdit(selected[i].feature)) {
                menuItem.set("disabled", true);
                return;
            }
        }

    },

    updateAnnotationInfoEditorMenuItem: function() {
        var menuItem = this.getMenuItem("annotation_info_editor");
        var selected = this.selectionManager.getSelection();
        var parent = this.getTopLevelAnnotation(selected[0].feature);
        for (var i = 1; i < selected.length; ++i) {
            if (this.getTopLevelAnnotation(selected[i].feature) != parent) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateUndoMenuItem: function() {
        var menuItem = this.getMenuItem("undo");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selected[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },

    updateRedoMenuItem: function() {
        var menuItem = this.getMenuItem("redo");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selected[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },


    updateHistoryMenuItem: function() {
        var menuItem = this.getMenuItem("history");
        var selected = this.selectionManager.getSelection();
        if (selected.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    }, 

    updateZoomToBaseLevelMenuItem: function() {
        var menuItem = this.getMenuItem("zoom_to_base_level");
        if( !this.gview.isZoomedToBase() ) {
            menuItem.set("label", "Zoom to base level");
        }
        else {
            menuItem.set("label", "Zoom back out");
        }
    },

    updateDuplicateMenuItem: function() {
        var menuItem = this.getMenuItem("duplicate");
        var selected = this.selectionManager.getSelection();
        var parent = this.getTopLevelAnnotation(selected[0].feature);
        for (var i = 1; i < selected.length; ++i) {
            if (this.getTopLevelAnnotation(selected[i].feature) != parent) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateSetAsFivePrimeEndMenuItem: function() {
        var menuItem = this.getMenuItem("set_as_five_prime_end");
        var selectedAnnots = this.selectionManager.getSelection();
        var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
        if (selectedAnnots.length > 1 || selectedEvidence.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedEvidence.length == 0) {
            menuItem.set("disabled", true);
            return;
        }
        if (!selectedAnnots[0].feature.parent() || !selectedEvidence[0].feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedAnnots[0].feature.get("strand") != selectedEvidence[0].feature.get("strand")) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },

    updateSetAsThreePrimeEndMenuItem: function() {
        var menuItem = this.getMenuItem("set_as_three_prime_end");
        var selectedAnnots = this.selectionManager.getSelection();
        var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
        if (selectedAnnots.length > 1 || selectedEvidence.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedEvidence.length == 0) {
            menuItem.set("disabled", true);
            return;
        }
        if (!selectedAnnots[0].feature.parent() || !selectedEvidence[0].feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedAnnots[0].feature.get("strand") != selectedEvidence[0].feature.get("strand")) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },

    updateSetBothEndsMenuItem: function() {
        var menuItem = this.getMenuItem("set_both_ends");
        var selectedAnnots = this.selectionManager.getSelection();
        var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
        if (selectedAnnots.length > 1 || selectedEvidence.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedEvidence.length == 0) {
            menuItem.set("disabled", true);
            return;
        }
        if (!selectedAnnots[0].feature.parent() || !selectedEvidence[0].feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (selectedAnnots[0].feature.get("strand") != selectedEvidence[0].feature.get("strand")) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },

    updateSetNextDonorMenuItem: function() {
        var menuItem = this.getMenuItem("set_downstream_donor");
        var selectedAnnots = this.selectionManager.getSelection();
        if (selectedAnnots.length != 1) {
            menuItem.set("disabled", true);
            return;
        }
        var feature = selectedAnnots[0].feature;
        if (!SequenceOntologyUtils.exonTerms[feature.get("type")]) {
            menuItem.set("disabled", true);
            return;
        }
        if (!feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        var subfeatures = feature.parent().get("subfeatures");
        var exons = [];
        for (var i = 0; i < subfeatures.length; ++i) {
            if (SequenceOntologyUtils.exonTerms[subfeatures[i].get("type")]) {
                exons.push(subfeatures[i]);
            }
        }
        this.sortAnnotationsByLocation(exons);
        if (feature.get("strand") == -1) {
            if (feature.id() == exons[0].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        else {
            if (feature.id() == exons[exons.length - 1].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateSetPreviousDonorMenuItem: function() {
        var menuItem = this.getMenuItem("set_upstream_donor");
        var selectedAnnots = this.selectionManager.getSelection();
        if (selectedAnnots.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        var feature = selectedAnnots[0].feature;
        if (!SequenceOntologyUtils.exonTerms[feature.get("type")]) {
            menuItem.set("disabled", true);
            return;
        }
        if (!feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        var subfeatures = feature.parent().get("subfeatures");
        var exons = [];
        for (var i = 0; i < subfeatures.length; ++i) {
            if (SequenceOntologyUtils.exonTerms[subfeatures[i].get("type")]) {
                exons.push(subfeatures[i]);
            }
        }
        this.sortAnnotationsByLocation(exons);
        if (feature.get("strand") == -1) {
            if (feature.id() == exons[0].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        else {
            if (feature.id() == exons[exons.length - 1].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateSetNextAcceptorMenuItem: function() {
        var menuItem = this.getMenuItem("set_downstream_acceptor");
        var selectedAnnots = this.selectionManager.getSelection();
        if (selectedAnnots.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        var feature = selectedAnnots[0].feature;
        if (!SequenceOntologyUtils.exonTerms[feature.get("type")]) {
            menuItem.set("disabled", true);
            return;
        }
        if (!feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        var subfeatures = feature.parent().get("subfeatures");
        var exons = [];
        for (var i = 0; i < subfeatures.length; ++i) {
            if (SequenceOntologyUtils.exonTerms[subfeatures[i].get("type")]) {
                exons.push(subfeatures[i]);
            }
        }
        this.sortAnnotationsByLocation(exons);
        if (feature.get("strand") == -1) {
            if (feature.id() == exons[exons.length - 1].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        else {
            if (feature.id() == exons[0].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },

    updateSetPreviousAcceptorMenuItem: function() {
        var menuItem = this.getMenuItem("set_upstream_acceptor");
        var selectedAnnots = this.selectionManager.getSelection();
        if (selectedAnnots.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        var feature = selectedAnnots[0].feature;
        if (!SequenceOntologyUtils.exonTerms[feature.get("type")]) {
            menuItem.set("disabled", true);
            return;
        }
        if (!feature.parent()) {
            menuItem.set("disabled", true);
            return;
        }
        if (!this.canEdit(selectedAnnots[0].feature)) {
            menuItem.set("disabled", true);
            return;
        }
        var subfeatures = feature.parent().get("subfeatures");
        var exons = [];
        for (var i = 0; i < subfeatures.length; ++i) {
            if (SequenceOntologyUtils.exonTerms[subfeatures[i].get("type")]) {
                exons.push(subfeatures[i]);
            }
        }
        this.sortAnnotationsByLocation(exons);
        if (feature.get("strand") == -1) {
            if (feature.id() == exons[exons.length - 1].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        else {
            if (feature.id() == exons[0].id()) {
                menuItem.set("disabled", true);
                return;
            }
        }
        menuItem.set("disabled", false);
    },
    
    updateLockAnnotationMenuItem: function() {
        var menuItem = this.getMenuItem("lock_annotation");
        var selectedAnnots = this.selectionManager.getSelection();
        if (selectedAnnots.length > 1) {
            menuItem.set("disabled", true);
            return;
        }
        var feature = this.getTopLevelAnnotation(selectedAnnots[0].feature);
        if (feature.get("locked")) {
            menuItem.set("label", "Unlock annotation");
        }
        else {
            menuItem.set("label", "Lock annotation");
        }
        if (feature.get("owner") != this.username && !this.isAdmin()) {
            menuItem.set("disabled", true);
            return;
        }
        menuItem.set("disabled", false);
    },

    getMenuItem: function(operation) {
        return annot_context_menu.getChildren()[contextMenuItems[operation]];
    },

    sortAnnotationsByLocation: function(annots) {
        var track = this;
        return annots.sort(function(annot1, annot2) {
                               var start1 = annot1.get("start");
                               var end1 = annot1.get("end");
                               var start2 = annot2.get("start");
                               var end2 = annot2.get('end');

                               if (start1 != start2)  { return start1 - start2; }
                               else if (end1 != end2) { return end1 - end2; }
                               else                   { return annot1.id().localeCompare(annot2.id()); }
                           });
    },



    
    executeUpdateOperation: function(postData, loadCallback) {
        var track = this;
        if (!this.listener || this.listener.fired != -1 ) {
            this.handleError({responseText: '{ error: "Server connection error - try reloading the page" }'});
            return;
        }
        xhr(context_path + "/AnnotationEditorService", {
            handleAs: "json",
            data: JSON.stringify(postData),
            method: "post"
        }).then(function(response, ioArgs) {
            if (loadCallback) {
                loadCallback(response);
            }
            if (response && response.alert) {
                alert(response.alert);
            }
        }, function(response, ioArgs) {
            track.handleError({responseText: response.response.text });
        });
    },

    isProteinCoding: function(feature) {
        var topLevelFeature = this.getTopLevelAnnotation(feature);
        if (topLevelFeature.afeature.parent_type && topLevelFeature.afeature.parent_type.name == "gene" && (topLevelFeature.get("type") == "transcript" || topLevelFeature.get("type") == "mRNA")) {
            return true;
        }
        return false;
    },

    isLoggedIn: function() {
        return this.username != undefined;
    },

    hasWritePermission: function() {
        return this.permission & Permission.WRITE;
    },
    
    isAdmin: function() {
        return this.permission & Permission.ADMIN;
    },
    
    canEdit: function(feature) {
        if (feature) {
            feature = this.getTopLevelAnnotation(feature);
        }
        return this.hasWritePermission() && (feature ? !feature.get("locked") : true);
    },

    processParent: function(feature, operation) {
        var parentId = feature.parent_id;
        if (parentId) {
            var topLevelFeatures = this.topLevelParents[parentId] || (this.topLevelParents[parentId] = {});
            switch (operation) {
            case "ADD":
                topLevelFeatures[feature.uniquename] = feature;
                break;
            case "DELETE":
                delete topLevelFeatures[feature.uniquename];
                break;
            case "UPDATE":
                topLevelFeatures[feature.uniquename] = feature;
                break;
            }
        }

    },

    getTopLevelAnnotation: function(annotation) {
        while( annotation.parent() ) {
            annotation = annotation.parent();
        }
        return annotation;
    }

});


return AnnotTrack;
});

/*
 * Copyright (c) 2010-2011 Berkeley Bioinformatics Open Projects (BBOP)
 * 
 * This package and its accompanying libraries are free software; you can
 * redistribute it and/or modify it under the terms of the LGPL (either version
 * 2.1, or at your option, any later version) or the Artistic License 2.0. Refer
 * to LICENSE for the full license text.
 * 
 */
