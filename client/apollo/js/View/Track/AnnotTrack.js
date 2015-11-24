define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'jquery',
        'jqueryui/draggable',
        'jqueryui/droppable',
        'jqueryui/resizable',
        'jqueryui/autocomplete',
        'jqueryui/dialog',
        'dijit/registry',
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
        'WebApollo/View/Track/DraggableHTMLFeatures',
        'WebApollo/FeatureSelectionManager',
        'WebApollo/JSONUtils',
        'WebApollo/BioFeatureUtils',
        'WebApollo/Permission',
        'WebApollo/SequenceSearch',
        'WebApollo/EUtils',
        'WebApollo/SequenceOntologyUtils',
        'JBrowse/Model/SimpleFeature',
        'JBrowse/Util',
        'JBrowse/View/GranularRectLayout',
        'dojo/request/xhr',
        'dojox/widget/Standby',
        'dijit/Tooltip',
        'WebApollo/FormatUtils',
        'dijit/form/Select',
        'dojo/store/Memory',
        'dojo/data/ObjectStore'
    ],
    function (declare,
              array,
              $,
              draggable,
              droppable,
              resizable,
              autocomplete,
              dialog,
              registry,
              dijitMenu,
              dijitMenuItem,
              dijitMenuSeparator,
              dijitPopupMenuItem,
              dijitButton,
              dijitDropDownButton,
              dijitDropDownMenu,
              dijitComboBox,
              dijitTextBox,
              dijitValidationTextBox,
              dijitRadioButton,
              dojoxDialogSimple,
              dojoxDataGrid,
              dojoxCells,
              dojoItemFileWriteStore,
              DraggableFeatureTrack,
              FeatureSelectionManager,
              JSONUtils,
              BioFeatureUtils,
              Permission,
              SequenceSearch,
              EUtils,
              SequenceOntologyUtils,
              SimpleFeature,
              Util,
              Layout,
              xhr,
              Standby,
              Tooltip,
              FormatUtils,
              Select,
              Memory,
              ObjectStore) {

        var listener;
        var client;

        var annot_context_menu;
        var contextMenuItems;

        var context_path = "..";

        var AnnotTrack = declare(DraggableFeatureTrack, {
            constructor: function (args) {
                this.isWebApolloAnnotTrack = true;
                this.has_custom_context_menu = true;
                this.exportAdapters = [];

                this.selectionManager = this.setSelectionManager(this.webapollo.annotSelectionManager);

                this.selectionClass = "selected-annotation";
                this.annot_under_mouse = null;

                /**
                 * only show residues overlay if "pointer-events" CSS property is
                 * supported (otherwise will interfere with passing of events to
                 * features beneath the overlay)
                 */
                this.useResiduesOverlay = 'pointerEvents' in document.body.style;
                this.FADEIN_RESIDUES = false;

                var thisB = this;

                this.annotMouseDown = function (event) {
                    thisB.onAnnotMouseDown(event);
                };

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


                this.gview.browser.subscribe("/jbrowse/v1/n/navigate", dojo.hitch(this, function (currRegion) {
                    if (currRegion.ref != this.refSeq.name) {

                        // we socket changes
                        if (this.listener) {
                            this.listener.close();
                        }
                    }

                }));

                this.gview.browser.setGlobalKeyboardShortcut('[', track, 'scrollToPreviousEdge');
                this.gview.browser.setGlobalKeyboardShortcut(']', track, 'scrollToNextEdge');

                this.gview.browser.setGlobalKeyboardShortcut('}', track, 'scrollToNextTopLevelFeature');
                this.gview.browser.setGlobalKeyboardShortcut('{', track, 'scrollToPreviousTopLevelFeature');

                this.topLevelParents = {};
            },

            renderExonSegments: function (subfeature, subDiv, cdsMin, cdsMax,
                                          displayStart, displayEnd, priorCdsLength, reverse) {
                var utrClass;
                var parentType = subfeature.parent().afeature.parent_type;
                if (!this.isProteinCoding(subfeature.parent())) {
                    var clsName = parentType && parentType.name == "pseudogene" ? "pseudogene" : subfeature.parent().get("type");
                    var cfg = this.config.style.alternateClasses[clsName];
                    utrClass = cfg.className;
                }
                return this.inherited(arguments, [subfeature, subDiv, cdsMin, cdsMax, displayStart, displayEnd, priorCdsLength, reverse, utrClass]);
            },

            _defaultConfig: function () {
                var thisConfig = this.inherited(arguments);
                thisConfig.menuTemplate = null;
                thisConfig.noExport = true;  // turn off default "Save track data" "
                thisConfig.style.centerChildrenVertically = false;
                thisConfig.pinned = true;
                return thisConfig;
            },


            setViewInfo: function (genomeView, numBlocks,
                                   trackDiv, labelDiv,
                                   widthPct, widthPx, scale) {

                this.inherited(arguments);
                var track = this;

                // to initAnnotContextMenu() once permissions are returned by server
                var success = this.getPermission(function () {
                    track.initAnnotContextMenu();
                });

                var standby = new Standby({
                    target: track.div,
                    color: "transparent",
                    image: "plugins/WebApollo/img/loading.gif"
                });
                document.body.appendChild(standby.domNode);
                standby.startup();
                standby.show();


                if (!this.webapollo.loginMenuInitialized && this.browser.config.show_nav && this.browser.config.show_menu) {
                    this.webapollo.initLoginMenu(this.username);
                }
                if (!this.webapollo.searchMenuInitialized && this.permission && this.browser.config.show_nav && this.browser.config.show_menu) {
                    this.webapollo.initSearchMenu();
                }
                this.initSaveMenu();
                this.initPopupDialog();

                if (success) {
                    track.createAnnotationChangeListener(0);

                    var query = {
                        "track": track.getUniqueTrackName(),
                        "operation": "get_features",
                        "organism": track.webapollo.organism
                    };

                    xhr(context_path + "/AnnotationEditorService", {
                        handleAs: "json",
                        data: JSON.stringify(query),
                        method: "post"
                    }).then(function (response, ioArgs) {
                        var responseFeatures = response.features;
                        if (!responseFeatures) {
                            alert("Error: " + JSON.stringify(response));
                            console.log(response);
                            return;
                        }

                        for (var i = 0; i < responseFeatures.length; i++) {
                            var jfeat = JSONUtils.createJBrowseFeature(responseFeatures[i]);
                            track.store.insert(jfeat);
                            track.processParent(responseFeatures[i], "ADD");
                        }
                        track.changed();
                        standby.hide();

                    }, function (response, ioArgs) {
                        console.log("Annotation server error--maybe you forgot to login to the server?");
                        track.handleError({responseText: response.response.text});
                        return response;
                    });
                }

                if (success) {
                    this.makeTrackDroppable();
                    this.hide();
                    this.show();
                }
                else {
                    this.hide();
                    if (this.browser.config.disableJBrowseMode) {
                        this.login();
                    }
                }

            },

            createAnnotationChangeListener: function (numTry) {
                //this.listener = new SockJS(context_path+"/stomp");
                var stomp_url = window.location.href;
                var index = stomp_url.search('/jbrowse');
                stomp_url = stomp_url.substr(0, index) + '/stomp/';
                this.listener = new SockJS(stomp_url);
                this.client = Stomp.over(this.listener);
                this.client.debug = function (str) {
                    if (this.verbose_server_notification) {
                        console.log(str);
                    }
                };
                var client = this.client;
                var track = this;
                var browser = this.gview.browser;

                if (typeof window.parent.getEmbeddedVersion == 'function') {
                    if (window.parent.getEmbeddedVersion() == 'ApolloGwt-2.0') {
                        console.log('Registering embedded system with ApolloGwt-2.0.');

                        browser.subscribe("/jbrowse/v1/n/navigate", dojo.hitch(this, function (currRegion) {
                            window.parent.handleNavigationEvent(JSON.stringify(currRegion));
                        }));


                        var sendTracks = function (trackList, visibleTrackNames, showLabels) {
                            var filteredTrackList = [];
                            for (var trackConfigIndex in trackList) {
                                var filteredTrack = {};
                                var trackConfig = trackList[trackConfigIndex];
                                var visible = visibleTrackNames.indexOf(trackConfig.label)>=0||showLabels.indexOf(trackConfig.label)>=0;
                                filteredTrack.label = trackConfig.label;
                                filteredTrack.key = trackConfig.key;
                                filteredTrack.name = trackConfig.name;
                                filteredTrack.type = trackConfig.type;
                                filteredTrack.urlTemplate = trackConfig.urlTemplate;
                                filteredTrack.visible = visible;
                                filteredTrackList.push(filteredTrack);
                            }

                            //console.log('AnnotTrack::returning filtered track list: ' + filteredTrackList.length);
                            window.parent.loadTracks(JSON.stringify(filteredTrackList));
                        };

                        var handleTrackVisibility = function (trackInfo) {
                            var command = trackInfo.command;
                            if (command == "show") {
                                browser.publish('/jbrowse/v1/v/tracks/show', [browser.trackConfigsByName[trackInfo.label]]);
                            }
                            else if (command == "hide") {
                                browser.publish('/jbrowse/v1/v/tracks/hide', [browser.trackConfigsByName[trackInfo.label]]);
                            }
                            else if (command == "list") {
                                var trackList = browser.trackConfigsByName;
                                var visibleTrackNames = browser.view.visibleTrackNames();
                                var showLabels = array.map(trackInfo.labels,function(track) { return track.label; });
                                sendTracks(trackList, visibleTrackNames, showLabels);
                            }
                            else {
                                console.log('unknown command: ' + command);
                            }
                        };
                        browser.subscribe('/jbrowse/v1/c/tracks/show', function(labels) { console.log("show update");handleTrackVisibility({command:"list",labels:labels}); });
                        browser.subscribe('/jbrowse/v1/c/tracks/hide', function() { console.log("hide update");handleTrackVisibility({command:"list"}); });
                        window.parent.registerFunction("handleTrackVisibility", handleTrackVisibility);


                        client.connect({}, function () {
                            // TODO: at some point enable "user" to websockets for chat, private notes, notify @someuser, etc.
                            var organism = JSON.parse(window.parent.getCurrentOrganism());
                            var sequence = JSON.parse(window.parent.getCurrentSequence());
                            var user = JSON.parse(window.parent.getCurrentUser());
                            client.subscribe("/topic/AnnotationNotification/" + organism.id + "/" + sequence.id, dojo.hitch(track, 'annotationNotification'));
                            client.subscribe("/topic/AnnotationNotification/user/" + user.email, dojo.hitch(track, 'annotationNotification'));
                        });
                        console.log('connection established');
                    }

                }
                else {
                    console.log('No embedded server is present.');

                    client.connect({}, function () {

                        var request = {
                            "name": track.refSeq.name,
                            "organism": track.webapollo.organism
                        };

                        xhr.post(context_path + "/sequence/lookupSequenceByNameAndOrganism/", {
                            data: JSON.stringify(request),
                            handleAs: "json"
                        }).then(function (response) {
                                if (response.error) {
                                    alert("Failed to subscribe to websocket, no seq/org id available");
                                    return;
                                }
                                if(typeof track.webapollo.organism == 'undefined'){
                                    track.webapollo.organism = response.organismId;
                                }
                                client.subscribe("/topic/AnnotationNotification/" + track.webapollo.organism + "/" + response.id, dojo.hitch(track, 'annotationNotification'));
                                client.subscribe("/topic/AnnotationNotification/user/" + track.username, dojo.hitch(track, 'annotationNotification'));
                            },
                            function () {
                                console.log("Received error in organism lookup, anonymous mode jbrowse");
                            });
                    });
                }
            },
            annotationNotification: function (message) {
                var track = this;
                var changeData;

                try {
                    changeData = JSON.parse(JSON.parse(message.body));

                    if (track.verbose_server_notification) {
                        console.log(changeData.operation + " command from server: ");
                        console.log(JSON.stringify(changeData));
                    }

                    if (changeData.operation == "logout" && changeData.username == track.username) {
                        alert("You have been logged out or your session has expired");
                        if(window.parent){
                            parent.location.reload();
                        }
                        else{
                            location.reload();
                        }

                        return;
                    }

                    if (changeData.operation == "ERROR" && changeData.username == track.username) {
                        alert(changeData.error_message);
                        console.log(changeData.error_message);
                        return;
                    }

                    if (changeData.operation == "ADD") {
                        if (changeData.sequenceAlterationEvent) {
                            track.getSequenceTrack().annotationsAddedNotification(changeData.features);
                        }
                        else {
                            track.annotationsAddedNotification(changeData.features);
                        }
                        if (typeof window.parent.getEmbeddedVersion == 'function') window.parent.handleFeatureAdded(JSON.stringify(changeData.features));
                    }
                    else if (changeData.operation == "DELETE") {
                        if (changeData.sequenceAlterationEvent) {
                            track.getSequenceTrack().annotationsDeletedNotification(changeData.features);
                        }
                        else {
                            track.annotationsDeletedNotification(changeData.features);
                        }
                        if (typeof window.parent.getEmbeddedVersion == 'function') window.parent.handleFeatureDeleted(JSON.stringify(changeData.features));
                    }
                    else if (changeData.operation == "UPDATE") {
                        if (changeData.sequenceAlterationEvent) {
                            track.getSequenceTrack().annotationsUpdatedNotification(changeData.features);
                        }
                        else {
                            track.annotationsUpdatedNotification(changeData.features);
                        }
                        if (typeof window.parent.getEmbeddedVersion == 'function') window.parent.handleFeatureDeleted(JSON.stringify(changeData.features));
                    }
                    else {
                        console.log('unknown command: ', changeData.operation);
                    }

                    track.changed();
                } catch (e) {
                    console.log(e);
                    console.log('Processing: ', message.body);
                }
            },

            /**
             * received notification from server ChangeNotificationListener that
             * annotations were added
             */
            annotationsAddedNotification: function (responseFeatures) {
                for (var i = 0; i < responseFeatures.length; ++i) {
                    var feat = JSONUtils.createJBrowseFeature(responseFeatures[i]);
                    var id = responseFeatures[i].uniquename;
                    if (!this.store.getFeatureById(id)) {
                        this.store.insert(feat);
                        this.processParent(responseFeatures[i], "ADD");
                    }
                }
            },

            /**
             * received notification from server ChangeNotificationListener that
             * annotations were deleted
             */
            annotationsDeletedNotification: function (responseFeatures) {
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
            annotationsUpdatedNotification: function (responseFeatures) {
                // this.annotationsDeletedNotification(annots);
                // this.annotationsAddedNotification(annots);
                var selfeats = this.selectionManager.getSelectedFeatures();

                for (var i = 0; i < responseFeatures.length; ++i) {
                    var id = responseFeatures[i].uniquename;
                    var feat = JSONUtils.createJBrowseFeature(responseFeatures[i]);
                    this.store.replace(feat);
                    this.processParent(responseFeatures[i], "UPDATE");
                }
            },

            /**
             * overriding renderFeature to add event handling right-click context menu
             */
            renderFeature: function (feature, uniqueId, block, scale, labelScale, descriptionScale,
                                     containerStart, containerEnd, history) {
                // if (uniqueId.length > 20) {
                //     feature.short_id = uniqueId;
                // }
                var track = this;
                // var featDiv = this.inherited( arguments );

                var rclass;
                var clsName;
                var type = feature.afeature.type;
                if (!this.isProteinCoding(feature)) {
                    var topLevelAnnotation = AnnotTrack.getTopLevelAnnotation(feature);
                    var parentType = feature.afeature.parent_type ? feature.afeature.parent_type.name : null;
                    var cfg = this.config.style.alternateClasses[feature.get("type")] || this.config.style.alternateClasses[parentType];
                    if (cfg) {
                        rclass = cfg.renderClassName;
                        if (!topLevelAnnotation.afeature.parent_type) {
                            clsName = cfg.className;
                        }
                    }
                }
                var featDiv = DraggableFeatureTrack.prototype.renderFeature.call(this, feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd, rclass, clsName);

                if (featDiv && featDiv != null && !history) {
                    annot_context_menu.bindDomNode(featDiv);
                    $(featDiv).droppable({
                        accept: ".selected-feature",   // only accept draggables that
                        // are selected feature divs
                        tolerance: "pointer",
                        hoverClass: "annot-drop-hover",
                        over: function (event, ui) {
                            track.annot_under_mouse = event.target;
                        },
                        out: function (event, ui) {
                            track.annot_under_mouse = null;
                        },
                        drop: function (event, ui) {
                            // ideally in the drop() on annot div is where would handle
                            // adding feature(s) to annot,
                            // but JQueryUI droppable doesn't actually call drop unless
                            // draggable helper div is actually
                            // over the droppable -- even if tolerance is set to pointer
                            // tolerance=pointer will trigger hover styling when over
                            // droppable,
                            // as well as call to over method (and out when leave
                            // droppable)
                            // BUT location of pointer still does not influence actual
                            // dropping and drop() call
                            // therefore getting around this by handling hover styling
                            // here based on pointer over annot,
                            // but drop-to-add part is handled by whole-track droppable,
                            // and uses annot_under_mouse
                            // tracking variable to determine if drop was actually on
                            // top of an annot instead of
                            // track whitespace
                            if (track.verbose_drop) {
                                console.log("dropped feature on annot:");
                                console.log(featDiv);
                            }
                        }
                    })
                        .click(function (event) {
                            if (event.altKey) {
                                track.getAnnotationInfoEditor();
                            }
                        })
                    ;
                }

                if (!history) {
                    var label = "Type: " + type.name + "<br/>Owner: " + feature.get("owner") + "<br/>Last modified: " + FormatUtils.formatDate(feature.afeature.date_last_modified) + " " + FormatUtils.formatTime(feature.afeature.date_last_modified);
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

            renderSubfeature: function (feature, featDiv, subfeature,
                                        displayStart, displayEnd, block) {
                var subdiv = this.inherited(arguments);

                if (this.canEdit(feature)) {
                    /**
                     * setting up annotation resizing via pulling of left/right edges but if
                     * subfeature is not selectable, do not bind mouse down
                     */
                    if (subdiv && subdiv != null && (!this.selectionManager.unselectableTypes[subfeature.get('type')])) {
                        $(subdiv).bind("mousedown", this.annotMouseDown);
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
            getSequenceTrack: function () {
                if (this.seqTrack) {
                    return this.seqTrack;
                }
                else {
                    var tracks = this.gview.tracks;
                    for (var i = 0; i < tracks.length; i++) {
                        // if (tracks[i] instanceof SequenceTrack) {
                        // if (tracks[i].config.type == "WebApollo/View/Track/AnnotSequenceTrack") {
                        if (tracks[i].isWebApolloSequenceTrack) {
                            this.seqTrack = tracks[i];
                            // tracks[i].setAnnotTrack(this);
                            break;
                        }
                    }
                }
                return this.seqTrack;
            },

            onFeatureMouseDown: function (event) {

                // _not_ calling DraggableFeatureTrack.prototyp.onFeatureMouseDown --
                // don't want to allow dragging (at least not yet)
                // event.stopPropagation();
                this.last_mousedown_event = event;
                var ftrack = this;
                if (ftrack.verbose_selection || ftrack.verbose_drag) {
                    console.log("AnnotTrack.onFeatureMouseDown called, genome coord: " + this.getGenomeCoord(event));
                }
                this.handleFeatureSelection(event);
            },

            /**
             * handles mouse down on an annotation subfeature to make the annotation
             * resizable by pulling the left/right edges
             */
            onAnnotMouseDown: function (event) {
                var track = this;
                // track.last_mousedown_event = event;
                var verbose_resize = track.verbose_resize;
                if (verbose_resize || track.verbose_mousedown) {
                    console.log("AnnotTrack.onAnnotMouseDown called");
                }
                event = event || window.event;
                var elem = (event.currentTarget || event.srcElement);
                // need to redo getLowestFeatureDiv
                // var featdiv = DraggableFeatureTrack.prototype.getLowestFeatureDiv(elem);
                var featdiv = track.getLowestFeatureDiv(elem);

                this.currentResizableFeature = featdiv.subfeature;
                this.makeResizable(featdiv);
                event.stopPropagation();
            },

            makeResizable: function (featdiv) {
                var track = this;
                var verbose_resize = this.verbose_resize;
                if (featdiv && (featdiv != null)) {
                    if (dojo.hasClass(featdiv, "ui-resizable")) {
                        if (verbose_resize) {
                            console.log("already resizable");
                            console.log(featdiv);
                        }
                    }
                    else {
                        if (verbose_resize) {
                            console.log("making annotation resizable");
                            console.log(featdiv);
                        }
                        var scale = track.gview.bpToPx(1);

                        // if zoomed int to showing sequence residues, then make
                        // edge-dragging snap to interbase pixels
                        var gridvals;
                        var charSize = track.webapollo.getSequenceCharacterSize();
                        if (scale === charSize.width) {
                            gridvals = [track.gview.charWidth, 1];
                        }
                        else {
                            gridvals = false;
                        }
                        $(featdiv).resizable({
                            handles: "e, w",
                            helper: "ui-resizable-helper",
                            autohide: false,
                            grid: gridvals,

                            stop: function (event, ui) {
                                if (verbose_resize) {
                                    console.log("resizable.stop() called, event:");
                                    console.dir(event);
                                    console.log("ui:");
                                    console.dir(ui);
                                }
                                var gview = track.gview;
                                var oldPos = ui.originalPosition;
                                var newPos = ui.position;
                                var oldSize = ui.originalSize;
                                var newSize = ui.size;
                                var leftDeltaPixels = newPos.left - oldPos.left;
                                var leftDeltaBases = Math.round(gview.pxToBp(leftDeltaPixels));
                                var oldRightEdge = oldPos.left + oldSize.width;
                                var newRightEdge = newPos.left + newSize.width;
                                var rightDeltaPixels = newRightEdge - oldRightEdge;
                                var rightDeltaBases = Math.round(gview.pxToBp(rightDeltaPixels));
                                if (verbose_resize) {
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
                                    "features": [
                                        {
                                            "uniquename": subfeat.getUniqueName(),
                                            "location": {
                                                "fmin": fmin, "fmax": fmax
                                            }
                                        }
                                    ],
                                    "operation": operation
                                };
                                track.executeUpdateOperation(JSON.stringify(postData));
                                track.changed();
                            }
                        });
                    }
                }
            },

            /**
             * feature click no-op (to override FeatureTrack.onFeatureClick, which
             * conflicts with mouse-down selection
             */
            onFeatureClick: function (event) {

                if (this.verbose_click) {
                    console.log("in AnnotTrack.onFeatureClick");
                }
                event = event || window.event;
                var elem = (event.currentTarget || event.srcElement);
                var featdiv = this.getLowestFeatureDiv(elem);
                if (featdiv && (featdiv != null)) {
                    if (this.verbose_click) {
                        console.log(featdiv);
                    }
                }
                // do nothing
                // event.stopPropagation();
            },

            /* feature_records ==> { feature: the_feature, track: track_feature_is_from } */
            addToAnnotation: function (annot, feature_records) {
                var target_track = this;

                var subfeats = [];
                var allSameStrand = 1;
                for (var i = 0; i < feature_records.length; ++i) {
                    var feature_record = feature_records[i];
                    var original_feat = feature_record.feature;
                    var feat = JSONUtils.makeSimpleFeature(original_feat);
                    var isSubfeature = !!feat.parent();  // !! is
                    // shorthand for
                    // returning
                    // true if value
                    // is defined
                    // and non-null
                    var annotStrand = annot.get('strand');
                    if (isSubfeature) {
                        var featStrand = feat.get('strand');
                        var featToAdd = feat;
                        if (featStrand != annotStrand) {
                            allSameStrand = 0;
                            featToAdd.set('strand', annotStrand);
                        }
                        subfeats.push(featToAdd);
                    }
                    else {  // top-level feature
                        var source_track = feature_record.track;
                        var subs = feat.get('subfeatures');
                        if (subs && subs.length > 0) {  // top-level
                            // feature with
                            // subfeatures
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
                            // $.merge(subfeats, subs);
                        }
                        else {  // top-level feature without subfeatures
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

                var featuresString = "";
                for (var i = 0; i < subfeats.length; ++i) {
                    var subfeat = subfeats[i];
                    // if (subfeat[target_track.subFields["type"]] !=
                    // "wholeCDS")
                    var source_track = subfeat.track;
                    if (subfeat.get('type') != "wholeCDS") {
                        var jsonFeature = JSONUtils.createApolloFeature(subfeats[i], "exon");
                        featuresString += ", " + JSON.stringify(jsonFeature);
                    }
                }
                // var parent = JSONUtils.createApolloFeature(annot, target_track.fields,
                // target_track.subfields);
                // parent.uniquename = annot[target_track.fields["name"]];
                var postData = '{ "track": "' + target_track.getUniqueTrackName() + '", "features": [ {"uniquename": "' + annot.id() + '"}' + featuresString + '], "operation": "add_exon" }';
                target_track.executeUpdateOperation(postData);
            },

            makeTrackDroppable: function () {
                var target_track = this;
                var target_trackdiv = target_track.div;
                if (target_track.verbose_drop) {
                    console.log("making track a droppable target: ");
                    console.log(this);
                    console.log(target_trackdiv);
                }
                $(target_trackdiv).droppable({
                    // only accept draggables that are selected feature divs
                    accept: ".selected-feature",
                    // switched to using deactivate() rather than drop() for drop
                    // handling
                    // this fixes bug where drop targets within track (feature divs)
                    // were lighting up as drop target,
                    // but dropping didn't actually call track.droppable.drop()
                    // (see explanation in feature droppable for why we catch drop at
                    // track div rather than feature div child)
                    // cause is possible bug in JQuery droppable where droppable over(),
                    // drop() and hoverclass
                    // collision calcs may be off (at least when tolerance=pointer)?
                    //
                    // Update 3/2012
                    // deactivate behavior changed? Now getting called every time
                    // dragged features are release,
                    // regardless of whether they are over this track or not
                    // so added another hack to get around drop problem
                    // combination of deactivate and keeping track via over()/out() of
                    // whether drag is above this track when released
                    // really need to look into actual drop calc fix -- maybe fixed in
                    // new JQuery releases?
                    //
                    // drop: function(event, ui) {
                    over: function (event, ui) {
                        target_track.track_under_mouse_drag = true;
                        if (target_track.verbose_drop) {
                            console.log("droppable entered AnnotTrack")
                        }
                        ;
                    },
                    out: function (event, ui) {
                        target_track.track_under_mouse_drag = false;
                        if (target_track.verbose_drop) {
                            console.log("droppable exited AnnotTrack")
                        }
                        ;
                    },
                    deactivate: function (event, ui) {
                        // console.log("trackdiv droppable detected: draggable
                        // deactivated");
                        // "this" is the div being dropped on, so same as
                        // target_trackdiv
                        if (target_track.verbose_drop) {
                            console.log("draggable deactivated");
                        }

                        var dropped_feats = target_track.webapollo.featSelectionManager.getSelection();
                        // problem with making individual annotations droppable, so
                        // checking for "drop" on annotation here,
                        // and if so re-routing to add to existing annotation
                        if (target_track.annot_under_mouse != null) {
                            if (target_track.verbose_drop) {
                                console.log("draggable dropped onto annot: ");
                                console.log(target_track.annot_under_mouse.feature);
                            }
                            target_track.addToAnnotation(target_track.annot_under_mouse.feature, dropped_feats);
                        }
                        else if (target_track.track_under_mouse_drag) {
                            if (target_track.verbose_drop) {
                                console.log("draggable dropped on AnnotTrack");
                            }
                            target_track.createAnnotations(dropped_feats);
                        }
                        // making sure annot_under_mouse is cleared
                        // (should do this in the drop? but need to make sure _not_ null
                        // when
                        target_track.annot_under_mouse = null;
                        target_track.track_under_mouse_drag = false;
                    }
                });
                if (target_track.verbose_drop) {
                    console.log("finished making droppable target");
                }
            },

            createAnnotations: function (selection_records) {
                console.log('createAnnotations');
                var target_track = this;
                var featuresToAdd = new Array();
                var parentFeatures = new Object();
                var subfeatures = [];
                var strand;
                var parentFeature;
                for (var i in selection_records) {
                    console.log('selection ' + i);
                    var dragfeat = selection_records[i].feature;
                    var is_subfeature = !!dragfeat.parent();  // !! is shorthand
                    // for returning
                    // true if value is
                    // defined and
                    // non-null

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
                        if (children) {
                            for (var j = 0; j < children.length; ++j) {
                                subfeatures.push(children[j]);
                            }
                        }
                        else {
                            subfeatures.push(dragfeat);
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
                        featureToAdd = new SimpleFeature({data: {strand: strand}});
                    }
                    if (!featureToAdd.get('name')) {
                        featureToAdd.set('name', featureToAdd.get('id'));
                    }
                    featureToAdd.set("strand", strand);
                    var fmin = undefined;
                    var fmax = undefined;
                    featureToAdd.set('subfeatures', new Array());
                    array.forEach(subfeatures, function (subfeature) {
                        if (!singleParent && SequenceOntologyUtils.cdsTerms[subfeature.get("type")]) {
                            return;
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
                        featureToAdd.get("subfeatures").push(dragfeat);
                    });
                    featureToAdd.set("start", fmin);
                    featureToAdd.set("end", fmax);
                    var afeat = JSONUtils.createApolloFeature(featureToAdd, "mRNA", true);
                    featuresToAdd.push(afeat);

                    var postData = {
                        "track": target_track.getUniqueTrackName(),
                        "features": featuresToAdd,
                        "operation": "add_transcript"
                    };
                    target_track.executeUpdateOperation(JSON.stringify(postData));
                };

                console.log('process: ' + strand);
                if (strand == -2) {
                    var content = dojo.create("div");
                    var message = dojo.create("div", {
                        className: "confirm_message",
                        innerHTML: "Creating annotation with subfeatures in opposing strands.  Choose strand:"
                    }, content);
                    var buttonsDiv = dojo.create("div", {className: "confirm_buttons"}, content);
                    var plusButton = dojo.create("button", {
                        className: "confirm_button",
                        innerHTML: "Plus"
                    }, buttonsDiv);
                    var minusButton = dojo.create("button", {
                        className: "confirm_button",
                        innerHTML: "Minus"
                    }, buttonsDiv);
                    var cancelButton = dojo.create("button", {
                        className: "confirm_button",
                        innerHTML: "Cancel"
                    }, buttonsDiv);
                    dojo.connect(plusButton, "onclick", function () {
                        strand = 1;
                        target_track.closeDialog();
                    });
                    dojo.connect(minusButton, "onclick", function () {
                        strand = -1;
                        target_track.closeDialog();
                    });
                    dojo.connect(cancelButton, "onclick", function () {
                        target_track.closeDialog();
                    });
                    var handle = dojo.connect(AnnotTrack.popupDialog, "onHide", function () {
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

            createGenericAnnotations: function (feats, type, subfeatType, topLevelType) {
                var target_track = this;
                var featuresToAdd = new Array();
                var parentFeatures = new Object();
                for (var i in feats) {
                    var dragfeat = feats[i];

                    var is_subfeature = !!dragfeat.parent();  // !! is shorthand
                    // for returning
                    // true if value is
                    // defined and
                    // non-null
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
                            featureToAdd.get("subfeatures").push(dragfeat);
                        }
                        featureToAdd.set("start", fmin);
                        featureToAdd.set("end", fmax);
                        var afeat = JSONUtils.createApolloFeature(featureToAdd, type, true, subfeatType);
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
                            var afeat = JSONUtils.createApolloFeature(dragfeat, type, true, subfeatType);
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
                var postData = '{ "track": "' + target_track.getUniqueTrackName() + '", "features": ' + JSON.stringify(featuresToAdd) + ', "operation": "add_feature" }';
                target_track.executeUpdateOperation(postData);
            },

            createGenericOneLevelAnnotations: function (feats, type, strandless) {
                var target_track = this;
                var featuresToAdd = new Array();
                var parentFeatures = new Object();
                for (var i in feats) {
                    var dragfeat = feats[i];

                    var is_subfeature = !!dragfeat.parent();  // !! is shorthand
                    // for returning
                    // true if value is
                    // defined and
                    // non-null
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
                            // featureToAdd.get("subfeatures").push( dragfeat );
                        }
                        featureToAdd.set("start", fmin);
                        featureToAdd.set("end", fmax);
                        if (strandless) {
                            featureToAdd.set("strand", 0);
                        }
                        var afeat = JSONUtils.createApolloFeature(featureToAdd, type, true);
                        /*
                         * if (topLevelType) { var topLevel = new Object();
                         * topLevel.location = dojo.clone(afeat.location);
                         * topLevel.type = dojo.clone(afeat.type);
                         * topLevel.type.name = topLevelType; topLevel.children =
                         * new Array(); topLevel.children.push(afeat); afeat =
                         * topLevel; }
                         */
                        featuresToAdd.push(afeat);
                    }
                    else {
                        for (var k = 0; k < featArray.length; ++k) {
                            var dragfeat = JSONUtils.makeSimpleFeature(featArray[k]);
                            var subfeat = dragfeat.get("subfeatures");
                            if (subfeat) subfeat.length = 0; // clear
                            // subfeatures
                            if (strandless) {
                                dragfeat.set("strand", 0);
                            }
                            dragfeat.set("name", featArray[k].get("name"));
                            var afeat = JSONUtils.createApolloFeature(dragfeat, type, true);
                            /*
                             * if (topLevelType) { var topLevel = new
                             * Object(); topLevel.location =
                             * dojo.clone(afeat.location); topLevel.type =
                             * dojo.clone(afeat.type); topLevel.type.name =
                             * topLevelType; topLevel.children = new
                             * Array(); topLevel.children.push(afeat); afeat =
                             * topLevel; }
                             */
                            featuresToAdd.push(afeat);
                        }
                    }
                }
                var postData = '{ "track": "' + target_track.getUniqueTrackName() + '", "features": ' + JSON.stringify(featuresToAdd) + ', "operation": "add_feature" }';
                target_track.executeUpdateOperation(postData);
            },

            duplicateSelectedFeatures: function () {
                var selected = this.selectionManager.getSelection();
                var selfeats = this.selectionManager.getSelectedFeatures();
                this.selectionManager.clearSelection();
                this.duplicateAnnotations(selfeats);
            },

            duplicateAnnotations: function (feats) {
                var track = this;
                var featuresToAdd = new Array();
                var subfeaturesToAdd = new Array();
                var proteinCoding = false;
                for (var i in feats) {
                    var feat = feats[i];
                    var is_subfeature = !!feat.parent();  // !! is shorthand
                                                          // for returning
                                                          // true if value is
                                                          // defined and
                                                          // non-null
                    if (is_subfeature) {
                        subfeaturesToAdd.push(feat);
                    }
                    else {
                        // featuresToAdd.push( JSONUtils.createApolloFeature( feat, "transcript") );
                        featuresToAdd.push(feat);
                    }
                }
                if (subfeaturesToAdd.length > 0) {
                    var feature = new SimpleFeature();
                    var subfeatures = new Array();
                    feature.set('subfeatures', subfeatures);
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
                    feature.set('start', fmin);
                    feature.set('end', fmax);
                    feature.set('strand', strand);
                    feature.set('type', subfeaturesToAdd[0].parent().get('type'));
                    feature.afeature = subfeaturesToAdd[0].parent().afeature;
                    // featuresToAdd.push( JSONUtils.createApolloFeature( feature, "transcript") );
                    featuresToAdd.push(feature);
                }
                for (var i = 0; i < featuresToAdd.length; ++i) {
                    var feature = featuresToAdd[i];
                    if (this.isProteinCoding(feature)) {
                        var feats = [JSONUtils.createApolloFeature(feature, "mRNA")];
                        var postData = '{ "track": "' + track.getUniqueTrackName() + '", "features": ' + JSON.stringify(feats) + ', "operation": "add_transcript" }';
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
            deleteSelectedFeatures: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.deleteAnnotations(selected);
            },

            deleteAnnotations: function (records) {
                var track = this;
                var features = '"features": [';
                var uniqueNames = [];
                var parents = {};
                var toBeDeleted = [];
                for (var i in records) {
                    var record = records[i];
                    var selfeat = record.feature;
                    var seltrack = record.track;
                    var uniqueName = selfeat.getUniqueName();
                    // just checking to ensure that all features in selection are from
                    // this track --
                    // if not, then don't try and delete them
                    if (seltrack === track) {
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
                    if (i > 0) {
                        features += ',';
                    }
                    features += ' { "uniquename": "' + toBeDeleted[i] + '" } ';
                    uniqueNames.push(toBeDeleted[i]);
                }
                features += ']';
                if (uniqueNames.length == 0) {
                    return;
                }
                if (this.verbose_delete) {
                    console.log("annotations to delete:");
                    console.log(features);
                }
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "delete_feature" }';
                track.executeUpdateOperation(postData);
            },

            mergeSelectedFeatures: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.mergeAnnotations(selected);
            },

            mergeAnnotations: function (selection) {
                var track = this;
                var annots = [];
                for (var i = 0; i < selection.length; i++) {
                    annots[i] = selection[i].feature;
                }

                var sortedAnnots = track.sortAnnotationsByLocation(annots);
                var leftAnnot = sortedAnnots[0];
                var rightAnnot = sortedAnnots[sortedAnnots.length - 1];
                var trackName = this.getUniqueTrackName();

                /*
                 * for (var i in annots) { var annot = annots[i]; // just checking to
                 * ensure that all features in selection are from this track -- // if
                 * not, then don't try and delete them if (annot.track === track) { var
                 * trackName = track.getUniqueTrackName(); if (leftAnnot == null ||
                 * annot[track.fields["start"]] < leftAnnot[track.fields["start"]]) {
                 * leftAnnot = annot; } if (rightAnnot == null ||
                 * annot[track.fields["end"]] > rightAnnot[track.fields["end"]]) {
                 * rightAnnot = annot; } } }
                 */

                var features;
                var operation;
                // merge exons
                if (leftAnnot.parent() && rightAnnot.parent() && leftAnnot.parent() == rightAnnot.parent()) {
                    features = '"features": [ { "uniquename": "' + leftAnnot.id() + '" }, { "uniquename": "' + rightAnnot.id() + '" } ]';
                    operation = "merge_exons";
                }
                // merge transcripts
                else {
                    var leftTranscriptId = leftAnnot.parent() ? leftAnnot.parent().id() : leftAnnot.id();
                    var rightTranscriptId = rightAnnot.parent() ? rightAnnot.parent().id() : rightAnnot.id();
                    features = '"features": [ { "uniquename": "' + leftTranscriptId + '" }, { "uniquename": "' + rightTranscriptId + '" } ]';
                    operation = "merge_transcripts";
                }
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            splitSelectedFeatures: function (event) {
                // var selected = this.selectionManager.getSelection();
                var selected = this.selectionManager.getSelectedFeatures();
                this.selectionManager.clearSelection();
                this.splitAnnotations(selected, event);
            },

            splitAnnotations: function (annots, event) {
                // can only split on max two elements
                if (annots.length > 2) {
                    return;
                }
                var track = this;
                var sortedAnnots = track.sortAnnotationsByLocation(annots);
                var leftAnnot = sortedAnnots[0];
                var rightAnnot = sortedAnnots[sortedAnnots.length - 1];
                var trackName = track.getUniqueTrackName();

                /*
                 * for (var i in annots) { var annot = annots[i]; // just checking to
                 * ensure that all features in selection are from this track -- // if
                 * not, then don't try and delete them if (annot.track === track) { var
                 * trackName = track.getUniqueTrackName(); if (leftAnnot == null ||
                 * annot[track.fields["start"]] < leftAnnot[track.fields["start"]]) {
                 * leftAnnot = annot; } if (rightAnnot == null ||
                 * annot[track.fields["end"]] > rightAnnot[track.fields["end"]]) {
                 * rightAnnot = annot; } } }
                 */
                var features;
                var operation;
                // split exon
                if (leftAnnot == rightAnnot) {
                    var coordinate = this.getGenomeCoord(event);
                    features = '"features": [ { "uniquename": "' + leftAnnot.id() + '", "location": { "fmax": ' + coordinate + ', "fmin": ' + (coordinate + 1) + ' } } ]';
                    operation = "split_exon";
                }
                // split transcript
                else if (leftAnnot.parent() == rightAnnot.parent()) {
                    features = '"features": [ { "uniquename": "' + leftAnnot.id() + '" }, { "uniquename": "' + rightAnnot.id() + '" } ]';
                    operation = "split_transcript";
                }
                else {
                    return;
                }
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            makeIntron: function (event) {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.makeIntronInExon(selected, event);
            },

            makeIntronInExon: function (records, event) {
                if (records.length > 1) {
                    return;
                }
                var track = this;
                var annot = records[0].feature;
                var coordinate = this.getGenomeCoord(event);
                var features = '"features": [ { "uniquename": "' + annot.id() + '", "location": { "fmin": ' + coordinate + ' } } ]';
                var operation = "make_intron";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            setTranslationStart: function (event, setStart) {
                // var selected = this.selectionManager.getSelection();
                var selfeats = this.selectionManager.getSelectedFeatures();
                this.selectionManager.clearSelection();
                this.setTranslationStartInCDS(selfeats, event);
            },

            setTranslationStartInCDS: function (annots, event) {
                if (annots.length > 1) {
                    return;
                }
                var track = this;
                var annot = annots[0];
                // var coordinate = this.gview.getGenomeCoord(event);
// var coordinate = Math.floor(this.gview.absXtoBp(event.pageX));
                var coordinate = this.getGenomeCoord(event);
                console.log("called setTranslationStartInCDS to: " + coordinate);

                var setStart = annot.parent() ? !annot.parent().get("manuallySetTranslationStart") : !annot.get("manuallySetTranslationStart");
                var uid = annot.parent() ? annot.parent().id() : annot.id();
                var features = '"features": [ { "uniquename": "' + uid + '"' + (setStart ? ', "location": { "fmin": ' + coordinate + ' }' : '') + ' } ]';
                var operation = "set_translation_start";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            setTranslationEnd: function (event) {
                // var selected = this.selectionManager.getSelection();
                var selfeats = this.selectionManager.getSelectedFeatures();
                this.selectionManager.clearSelection();
                this.setTranslationEndInCDS(selfeats, event);
            },

            setTranslationEndInCDS: function (annots, event) {
                if (annots.length > 1) {
                    return;
                }
                var track = this;
                var annot = annots[0];
                // var coordinate = this.gview.getGenomeCoord(event);
// var coordinate = Math.floor(this.gview.absXtoBp(event.pageX));
                var coordinate = this.getGenomeCoord(event);
                console.log("called setTranslationEndInCDS to: " + coordinate);

                var setEnd = annot.parent() ? !annot.parent().get("manuallySetTranslationEnd") : !annot.get("manuallySetTranslationEnd");
                var uid = annot.parent() ? annot.parent().id() : annot.id();
                var features = '"features": [ { "uniquename": "' + uid + '"' + (setEnd ? ', "location": { "fmax": ' + coordinate + ' }' : '') + ' } ]';
                var operation = "set_translation_end";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            flipStrand: function () {
                var selected = this.selectionManager.getSelection();
                this.flipStrandForSelectedFeatures(selected);
            },

            flipStrandForSelectedFeatures: function (records) {
                var track = this;
                var uniqueNames = new Object();
                for (var i in records) {
                    var record = records[i];
                    var selfeat = record.feature;
                    var seltrack = record.track;
                    var topfeat = AnnotTrack.getTopLevelAnnotation(selfeat);
                    var uniqueName = topfeat.id();
                    // just checking to ensure that all features in selection are from
                    // this track
                    if (seltrack === track) {
                        uniqueNames[uniqueName] = 1;
                    }
                }
                var features = '"features": [';
                var i = 0;
                for (var uniqueName in uniqueNames) {
                    var trackdiv = track.div;
                    var trackName = track.getUniqueTrackName();

                    if (i > 0) {
                        features += ',';
                    }
                    features += ' { "uniquename": "' + uniqueName + '" } ';
                    ++i;
                }
                features += ']';
                var operation = "flip_strand";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            setLongestORF: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.setLongestORFForSelectedFeatures(selected);
            },

            setLongestORFForSelectedFeatures: function (selection) {
                var track = this;
                var features = '"features": [';
                for (var i in selection) {
                    var annot = AnnotTrack.getTopLevelAnnotation(selection[i].feature);
                    var atrack = selection[i].track;
                    var uniqueName = annot.id();
                    // just checking to ensure that all features in selection are from
                    // this track
                    if (atrack === track) {
                        var trackdiv = track.div;
                        var trackName = track.getUniqueTrackName();

                        if (i > 0) {
                            features += ',';
                        }
                        features += ' { "uniquename": "' + uniqueName + '" } ';
                    }
                }
                features += ']';
                var operation = "set_longest_orf";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                track.executeUpdateOperation(postData);
            },

            setReadthroughStopCodon: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.setReadthroughStopCodonForSelectedFeatures(selected);
            },

            setReadthroughStopCodonForSelectedFeatures: function (selection) {
                var track = this;
                var features = '"features": [';
                for (var i in selection) {
                    var annot = AnnotTrack.getTopLevelAnnotation(selection[i].feature);
                    var atrack = selection[i].track;
                    var uniqueName = annot.id();
                    // just checking to ensure that all features in selection are from
                    // this track
                    if (atrack === track) {
                        var trackdiv = track.div;
                        var trackName = track.getUniqueTrackName();

                        if (i > 0) {
                            features += ',';
                        }
                        features += ' { "uniquename": "' + uniqueName + '", "readthrough_stop_codon": ' + !annot.data.readThroughStopCodon + '} ';
                    }
                }
                features += ']';
                var operation = "set_readthrough_stop_codon";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setAsFivePrimeEnd: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.webapollo.featSelectionManager.clearSelection();
                this.setAsFivePrimeEndForSelectedFeatures(selectedAnnots, selectedEvidence);
            },

            setAsFivePrimeEndForSelectedFeatures: function (selectedAnnots, selectedEvidence) {
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
                var features = '"features": [ { "uniquename": "' + uniqueName + '", "location": { "fmin": ' + fmin + ', "fmax": ' + fmax + ' } } ]';
                var operation = "set_exon_boundaries";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setAsThreePrimeEnd: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.webapollo.featSelectionManager.clearSelection();
                this.setAsThreePrimeEndForSelectedFeatures(selectedAnnots, selectedEvidence);
            },

            setAsThreePrimeEndForSelectedFeatures: function (selectedAnnots, selectedEvidence) {
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
                var features = '"features": [ { "uniquename": "' + uniqueName + '", "location": { "fmin": ' + fmin + ', "fmax": ' + fmax + ' } } ]';
                var operation = "set_exon_boundaries";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setBothEnds: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                var selectedEvidence = this.webapollo.featSelectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.webapollo.featSelectionManager.clearSelection();
                this.setBothEndsForSelectedFeatures(selectedAnnots, selectedEvidence);
            },

            setBothEndsForSelectedFeatures: function (selectedAnnots, selectedEvidence) {
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
                var features = '"features": [ { "uniquename": "' + uniqueName + '", "location": { "fmin": ' + fmin + ', "fmax": ' + fmax + ' } } ]';
                var operation = "set_exon_boundaries";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setToDownstreamDonor: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.setToDownstreamDonorForSelectedFeatures(selectedAnnots);
            },

            setToDownstreamDonorForSelectedFeatures: function (selectedAnnots) {
                var track = this;
                var annot = selectedAnnots[0].feature;
                var uniqueName = annot.id();
                var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                var operation = "set_to_downstream_donor";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setToUpstreamDonor: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.setToUpstreamDonorForSelectedFeatures(selectedAnnots);
            },

            setToUpstreamDonorForSelectedFeatures: function (selectedAnnots) {
                var track = this;
                var annot = selectedAnnots[0].feature;
                var uniqueName = annot.id();
                var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                var operation = "set_to_upstream_donor";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setToDownstreamAcceptor: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.setToDownstreamAcceptorForSelectedFeatures(selectedAnnots);
            },

            setToDownstreamAcceptorForSelectedFeatures: function (selectedAnnots) {
                var track = this;
                var annot = selectedAnnots[0].feature;
                var uniqueName = annot.id();
                var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                var operation = "set_to_downstream_acceptor";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            setToUpstreamAcceptor: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.setToUpstreamAcceptorForSelectedFeatures(selectedAnnots);
            },

            setToUpstreamAcceptorForSelectedFeatures: function (selectedAnnots) {
                var track = this;
                var annot = selectedAnnots[0].feature;
                var uniqueName = annot.id();
                var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                var operation = "set_to_upstream_acceptor";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            lockAnnotation: function () {
                var selectedAnnots = this.selectionManager.getSelection();
                this.selectionManager.clearAllSelection();
                this.lockAnnotationForSelectedFeatures(selectedAnnots);
            },

            lockAnnotationForSelectedFeatures: function (selectedAnnots) {
                var track = this;
                var annot = AnnotTrack.getTopLevelAnnotation(selectedAnnots[0].feature);
                var uniqueName = annot.id();
                var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                var operation = annot.get("locked") ? "unlock_feature" : "lock_feature";
                var trackName = track.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"}';
                track.executeUpdateOperation(postData);
            },

            getAnnotationInfoEditor: function () {
                var selected = this.selectionManager.getSelection();
                this.getAnnotationInfoEditorForSelectedFeatures(selected);
            },

            getAnnotationInfoEditorForSelectedFeatures: function (records) {
                var track = this;
                var record = records[0];
                var annot = AnnotTrack.getTopLevelAnnotation(record.feature);
                var seltrack = record.track;
                // just checking to ensure that all features in selection are from this
                // track
                if (seltrack !== track) {
                    return;
                }
                track.getAnnotationInfoEditorConfigs(track.getUniqueTrackName());
                var content = dojo.create("div", {'class': "annotation_info_editor_container"});
                if (annot.afeature.parent_id) {
                    var selectorDiv = dojo.create("div", {'class': "annotation_info_editor_selector"}, content);
                    var selectorLabel = dojo.create("label", {
                        innerHTML: "Select " + annot.get("type"),
                        'class': "annotation_info_editor_selector_label"
                    }, selectorDiv);
                    var data = [];
                    var feats = track.topLevelParents[annot.afeature.parent_id];
                    for (var i in feats) {
                        var feat = feats[i];
                        data.push({id: feat.uniquename, label: feat.name});
                    }
                    var store = new Memory({
                        data: data
                    });
                    var os = new ObjectStore({objectStore: store});
                    var selector = new Select({store: os});
                    selector.placeAt(selectorDiv);
                    selector.attr("value", annot.afeature.uniquename);
                    selector.attr("style", "width: 50%;");
                    var first = true;
                    dojo.connect(selector, "onChange", function (id) {
                        if (!first) {
                            dojo.destroy("child_annotation_info_editor");
                            annotContent = track.createAnnotationInfoEditorPanelForFeature(id, track.getUniqueTrackName(), selector, true);
                            dojo.attr(annotContent, "class", "annotation_info_editor");
                            dojo.attr(annotContent, "id", "child_annotation_info_editor");
                            dojo.place(annotContent, content);
                        }
                        first = false;
                    });
                }
                var numItems = 0;
                // if annotation has parent, get comments for parent
                if (annot.afeature.parent_id) {
                    var parentContent = this.createAnnotationInfoEditorPanelForFeature(annot.afeature.parent_id, track.getUniqueTrackName());
                    dojo.attr(parentContent, "class", "parent_annotation_info_editor");
                    dojo.place(parentContent, content);
                    ++numItems;
                }
                var annotContent = this.createAnnotationInfoEditorPanelForFeature(annot.id(), track.getUniqueTrackName(), selector, false);
                dojo.attr(annotContent, "class", "annotation_info_editor");
                dojo.attr(annotContent, "id", "child_annotation_info_editor");
                dojo.place(annotContent, content);
                ++numItems;
                dojo.attr(content, "style", "width:" + (numItems == 1 ? "28" : "58") + "em;");
                track.openDialog("Information Editor", content);
                AnnotTrack.popupDialog.resize();
                AnnotTrack.popupDialog._position();
            },

            getAnnotationInfoEditorConfigs: function (trackName) {
                var track = this;
                if (track.annotationInfoEditorConfigs) {
                    return;
                }
                var operation = "get_annotation_info_editor_configuration";
                var postData = {"track": trackName, "operation": operation};
                xhr.post(context_path + "/AnnotationEditorService", {
                    sync: true,
                    data: JSON.stringify(postData),
                    handleAs: "json",
                    timeout: 5000 * 1000
                }).then(function (response) {
                        track.annotationInfoEditorConfigs = {};
                        for (var i = 0; i < response.annotation_info_editor_configs.length; ++i) {
                            var config = response.annotation_info_editor_configs[i];
                            for (var j = 0; j < config.supported_types.length; ++j) {
                                track.annotationInfoEditorConfigs[config.supported_types[j]] = config;
                            }
                        }
                    }
                );
            },


            createAnnotationInfoEditorPanelForFeatureSideBar: function (uniqueName, trackName, selector, reload) {
                console.log("createAnnotationInfoEditorPanelForFeatureSideBar");
                var track = this;
                var hasWritePermission = this.canEdit(this.store.getFeatureById(uniqueName));
                var content = dojo.create("span");
                var header = dojo.create("div", {className: "annotation_sidebar_header"}, content);

                var nameDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var nameLabel = dojo.create("label", {
                    innerHTML: "Name",
                    'class': "annotation_info_editor_label"
                }, nameDiv);
                var nameField = new dijitTextBox({'class': "annotation_editor_field"});
                dojo.place(nameField.domNode, nameDiv);

                var symbolDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var symbolLabel = dojo.create("label", {
                    innerHTML: "Symbol",
                    'class': "annotation_info_editor_label"
                }, symbolDiv);
                var symbolField = new dijitTextBox({'class': "annotation_editor_field"});
                dojo.place(symbolField.domNode, symbolDiv);

                var descriptionDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var descriptionLabel = dojo.create("label", {
                    innerHTML: "Description",
                    'class': "annotation_info_editor_label"
                }, descriptionDiv);
                var descriptionField = new dijitTextBox({'class': "annotation_editor_field"});
                dojo.place(descriptionField.domNode, descriptionDiv);

                var dateCreationDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var dateCreationLabel = dojo.create("label", {
                    innerHTML: "Created",
                    'class': "annotation_info_editor_label"
                }, dateCreationDiv);
                var dateCreationField = new dijitTextBox({'class': "annotation_editor_field", readonly: true});
                dojo.place(dateCreationField.domNode, dateCreationDiv);

                var dateLastModifiedDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var dateLastModifiedLabel = dojo.create("label", {
                    innerHTML: "Last modified",
                    'class': "annotation_info_editor_label"
                }, dateLastModifiedDiv);
                var dateLastModifiedField = new dijitTextBox({'class': "annotation_editor_field", readonly: true});
                dojo.place(dateLastModifiedField.domNode, dateLastModifiedDiv);

                var statusDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var statusLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "Status"
                }, statusDiv);
                var statusFlags = dojo.create("div", {'class': "status"}, statusDiv);
                var statusRadios = new Object();

                if (!hasWritePermission) {
                    nameField.set("disabled", true);
                }
                var timeout = 100;

                var escapeString = function (str) {
                    return str.replace(/(["'])/g, "\\$1");
                };

                function init() {
                    var features = [{"uniquename": uniqueName}];
                    var operation = "get_annotation_info_editor_data";
                    var postData = {"track": trackName, features: features, "operation": operation};
                    dojo.xhrPost({
                        sync: true,
                        postData: JSON.stringify(postData),
                        url: context_path + "/AnnotationEditorService",
                        handleAs: "json",
                        timeout: 5000 * 1000, // Time in milliseconds
                        load: function (response, ioArgs) {
                            var feature = response.features[0];
                            var config = track.annotationInfoEditorConfigs[feature.type.cv.name + ":" + feature.type.name] || track.annotationInfoEditorConfigs["default"];
                            initName(feature);
                            initSymbol(feature);
                            initDescription(feature);
                            initDates(feature);
                            initStatus(feature, config);
                        }
                    });
                };
                var initName = function (feature) {
                    if (feature.name) {
                        nameField.set("value", feature.name);
                    }
                    var oldName;
                    dojo.connect(nameField, "onFocus", function () {
                        oldName = nameField.get("value");
                    });
                    dojo.connect(nameField, "onBlur", function () {
                        var newName = nameField.get("value");
                        if (oldName != newName) {
                            updateName(newName);
                            if (selector) {
                                var select = selector.store.get(feature.uniquename).then(function (select) {
                                    selector.store.setValue(select, "label", newName);
                                });
                            }
                        }
                    });
                };

                var initSymbol = function (feature) {
                    if (feature.symbol) {
                        symbolField.set("value", feature.symbol);
                    }
                    var oldSymbol;
                    dojo.connect(symbolField, "onFocus", function () {
                        oldSymbol = symbolField.get("value");
                    });
                    dojo.connect(symbolField, "onBlur", function () {
                        var newSymbol = symbolField.get("value");
                        if (oldSymbol != newSymbol) {
                            updateSymbol(newSymbol);
                        }
                    });
                };

                var initDescription = function (feature) {
                    if (feature.description) {
                        descriptionField.set("value", feature.description);
                    }
                    var oldDescription;
                    dojo.connect(descriptionField, "onFocus", function () {
                        oldDescription = descriptionField.get("value");
                    });
                    dojo.connect(descriptionField, "onBlur", function () {
                        var newDescription = descriptionField.get("value");
                        if (oldDescription != newDescription) {
                            updateDescription(newDescription);
                        }
                    });
                };
                var initDates = function (feature) {
                    if (feature.date_creation) {
                        dateCreationField.set("value", FormatUtils.formatDate(feature.date_creation));
                    }
                    if (feature.date_last_modified) {
                        dateLastModifiedField.set("value", FormatUtils.formatDate(feature.date_last_modified));
                    }
                };

                var initStatus = function (feature, config) {
                    var maxLength = 0;
                    var status = config.status;
                    if (status) {
                        for (var i = 0; i < status.length; ++i) {
                            if (status[i].length > maxLength) {
                                maxLength = status[i].length;
                            }
                        }
                        for (var i = 0; i < status.length; ++i) {
                            var statusRadioDiv = dojo.create("span", {
                                'class': "annotation_info_editor_radio",
                                style: "width:" + (maxLength * 0.75) + "em;"
                            }, statusFlags);
                            var statusRadio = new dijitRadioButton({
                                value: status[i],
                                name: "status_" + uniqueName,
                                checked: status[i] == feature.status ? true : false
                            });
                            if (!hasWritePermission) {
                                statusRadio.set("disabled", true);
                            }
                            dojo.place(statusRadio.domNode, statusRadioDiv);
                            var statusLabel = dojo.create("label", {
                                innerHTML: status[i],
                                'class': "annotation_info_editor_radio_label"
                            }, statusRadioDiv);
                            statusRadios[status[i]] = statusRadio;
                            dojo.connect(statusRadio, "onMouseDown", function (div, radio, label) {
                                return function (event) {
                                    if (radio.checked) {
                                        deleteStatus();
                                        dojo.place(new dijitRadioButton({
                                            value: status[i],
                                            name: "status_" + uniqueName,
                                            checked: false
                                        }).domNode, radio.domNode, "replace");
                                    }
                                };
                            }(statusRadioDiv, statusRadio, statusLabel));
                            dojo.connect(statusRadio, "onChange", function (label) {
                                return function (selected) {
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
                init();
                return content;
            },
            createAnnotationInfoEditorPanelForFeature: function (uniqueName, trackName, selector, reload) {
                var track = this;
//      var hasWritePermission = this.hasWritePermission();
                var hasWritePermission = this.canEdit(this.store.getFeatureById(uniqueName));
                var content = dojo.create("span");

                var header = dojo.create("div", {className: "annotation_info_editor_header"}, content);

                var nameDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var nameLabel = dojo.create("label", {
                    innerHTML: "Name",
                    'class': "annotation_info_editor_label"
                }, nameDiv);
                var nameField = new dijitTextBox({'class': "annotation_editor_field"});
                dojo.place(nameField.domNode, nameDiv);
                // var nameField = new dojo.create("input", { type: "text" }, nameDiv);

                var symbolDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var symbolLabel = dojo.create("label", {
                    innerHTML: "Symbol",
                    'class': "annotation_info_editor_label"
                }, symbolDiv);
                var symbolField = new dijitTextBox({'class': "annotation_editor_field"});
                dojo.place(symbolField.domNode, symbolDiv);

                var descriptionDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var descriptionLabel = dojo.create("label", {
                    innerHTML: "Description",
                    'class': "annotation_info_editor_label"
                }, descriptionDiv);
                var descriptionField = new dijitTextBox({'class': "annotation_editor_field"});
                dojo.place(descriptionField.domNode, descriptionDiv);

                var dateCreationDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var dateCreationLabel = dojo.create("label", {
                    innerHTML: "Created",
                    'class': "annotation_info_editor_label"
                }, dateCreationDiv);
                var dateCreationField = new dijitTextBox({'class': "annotation_editor_field", readonly: true});
                dojo.place(dateCreationField.domNode, dateCreationDiv);

                var dateLastModifiedDiv = dojo.create("div", {'class': "annotation_info_editor_field_section"}, content);
                var dateLastModifiedLabel = dojo.create("label", {
                    innerHTML: "Last modified",
                    'class': "annotation_info_editor_label"
                }, dateLastModifiedDiv);
                var dateLastModifiedField = new dijitTextBox({'class': "annotation_editor_field", readonly: true});
                dojo.place(dateLastModifiedField.domNode, dateLastModifiedDiv);

                var statusDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var statusLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "Status"
                }, statusDiv);
                var statusFlags = dojo.create("div", {'class': "status"}, statusDiv);
                var statusRadios = new Object();

                var dbxrefsDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var dbxrefsLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "DBXRefs"
                }, dbxrefsDiv);
                var dbxrefsTable = dojo.create("div", {
                    'class': "dbxrefs",
                    id: "dbxrefs_" + (selector ? "child" : "parent")
                }, dbxrefsDiv);
                var dbxrefButtonsContainer = dojo.create("div", {style: "text-align: center;"}, dbxrefsDiv);
                var dbxrefButtons = dojo.create("div", {'class': "annotation_info_editor_button_group"}, dbxrefButtonsContainer);
                var addDbxrefButton = dojo.create("button", {
                    innerHTML: "Add",
                    'class': "annotation_info_editor_button"
                }, dbxrefButtons);
                var deleteDbxrefButton = dojo.create("button", {
                    innerHTML: "Delete",
                    'class': "annotation_info_editor_button"
                }, dbxrefButtons);

                var attributesDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var attributesLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "Attributes"
                }, attributesDiv);
                var attributesTable = dojo.create("div", {
                    'class': "attributes",
                    id: "attributes_" + (selector ? "child" : "parent")
                }, attributesDiv);
                var attributeButtonsContainer = dojo.create("div", {style: "text-align: center;"}, attributesDiv);
                var attributeButtons = dojo.create("div", {'class': "annotation_info_editor_button_group"}, attributeButtonsContainer);
                var addAttributeButton = dojo.create("button", {
                    innerHTML: "Add",
                    'class': "annotation_info_editor_button"
                }, attributeButtons);
                var deleteAttributeButton = dojo.create("button", {
                    innerHTML: "Delete",
                    'class': "annotation_info_editor_button"
                }, attributeButtons);

                var pubmedIdsDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var pubmedIdsLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "Pubmed IDs"
                }, pubmedIdsDiv);
                var pubmedIdsTable = dojo.create("div", {
                    'class': "pubmed_ids",
                    id: "pubmd_ids_" + (selector ? "child" : "parent")
                }, pubmedIdsDiv);
                var pubmedIdButtonsContainer = dojo.create("div", {style: "text-align: center;"}, pubmedIdsDiv);
                var pubmedIdButtons = dojo.create("div", {'class': "annotation_info_editor_button_group"}, pubmedIdButtonsContainer);
                var addPubmedIdButton = dojo.create("button", {
                    innerHTML: "Add",
                    'class': "annotation_info_editor_button"
                }, pubmedIdButtons);
                var deletePubmedIdButton = dojo.create("button", {
                    innerHTML: "Delete",
                    'class': "annotation_info_editor_button"
                }, pubmedIdButtons);

                var goIdsDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var goIdsLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "Gene Ontology IDs"
                }, goIdsDiv);
                var goIdsTable = dojo.create("div", {
                    'class': "go_ids",
                    id: "go_ids_" + (selector ? "child" : "parent")
                }, goIdsDiv);
                var goIdButtonsContainer = dojo.create("div", {style: "text-align: center;"}, goIdsDiv);
                var goIdButtons = dojo.create("div", {'class': "annotation_info_editor_button_group"}, goIdButtonsContainer);
                var addGoIdButton = dojo.create("button", {
                    innerHTML: "Add",
                    'class': "annotation_info_editor_button"
                }, goIdButtons);
                var deleteGoIdButton = dojo.create("button", {
                    innerHTML: "Delete",
                    'class': "annotation_info_editor_button"
                }, goIdButtons);

                var commentsDiv = dojo.create("div", {'class': "annotation_info_editor_section"}, content);
                var commentsLabel = dojo.create("div", {
                    'class': "annotation_info_editor_section_header",
                    innerHTML: "Comments"
                }, commentsDiv);
                var commentsTable = dojo.create("div", {
                    'class': "comments",
                    id: "comments_" + (selector ? "child" : "parent")
                }, commentsDiv);
                var commentButtonsContainer = dojo.create("div", {style: "text-align: center;"}, commentsDiv);
                var commentButtons = dojo.create("div", {'class': "annotation_info_editor_button_group"}, commentButtonsContainer);
                var addCommentButton = dojo.create("button", {
                    innerHTML: "Add",
                    'class': "annotation_info_editor_button"
                }, commentButtons);
                var deleteCommentButton = dojo.create("button", {
                    innerHTML: "Delete",
                    'class': "annotation_info_editor_button"
                }, commentButtons);

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

                var escapeString = function (str) {
                    return str.replace(/(["'])/g, "\\$1");
                };

                function init() {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                    var operation = "get_annotation_info_editor_data";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    dojo.xhrPost({
                        sync: true,
                        postData: postData,
                        url: context_path + "/AnnotationEditorService",
                        handleAs: "json",
                        timeout: 5000 * 1000, // Time in milliseconds
                        load: function (response, ioArgs) {
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
                        setTimeout(function () {
                            initTable(domNode, tableNode, table, timeout);
                            return;
                        }, timeout);
                        return;
                    }
                    dojo.place(domNode, tableNode, "first");
                    table.startup();
                }

                var initType = function (feature) {
                    header.innerHTML = feature.type.name;
                };

                var initName = function (feature) {
                    if (feature.name) {
                        nameField.set("value", feature.name);
                    }
                    var oldName;
                    dojo.connect(nameField, "onFocus", function () {
                        oldName = nameField.get("value");
                    });
                    dojo.connect(nameField, "onBlur", function () {
                        var newName = nameField.get("value");
                        if (oldName != newName) {
                            updateName(newName);
                            if (selector) {
                                var select = selector.store.get(feature.uniquename).then(function (select) {
                                    selector.store.setValue(select, "label", newName);
                                });
                            }
                        }
                    });
                };

                var initSymbol = function (feature) {
                    if (feature.symbol) {
                        symbolField.set("value", feature.symbol);
                    }
                    var oldSymbol;
                    dojo.connect(symbolField, "onFocus", function () {
                        oldSymbol = symbolField.get("value");
                    });
                    dojo.connect(symbolField, "onBlur", function () {
                        var newSymbol = symbolField.get("value");
                        if (oldSymbol != newSymbol) {
                            updateSymbol(newSymbol);
                        }
                    });
                };

                var initDescription = function (feature) {
                    if (feature.description) {
                        descriptionField.set("value", feature.description);
                    }
                    var oldDescription;
                    dojo.connect(descriptionField, "onFocus", function () {
                        oldDescription = descriptionField.get("value");
                    });
                    dojo.connect(descriptionField, "onBlur", function () {
                        var newDescription = descriptionField.get("value");
                        if (oldDescription != newDescription) {
                            updateDescription(newDescription);
                        }
                    });
                };

                var initDates = function (feature) {
                    if (feature.date_creation) {
                        dateCreationField.set("value", FormatUtils.formatDate(feature.date_creation));
                    }
                    if (feature.date_last_modified) {
                        dateLastModifiedField.set("value", FormatUtils.formatDate(feature.date_last_modified));
                    }
                };

                var initStatus = function (feature, config) {
                    var maxLength = 0;
                    var status = config.status;
                    if (status) {
                        for (var i = 0; i < status.length; ++i) {
                            if (status[i].length > maxLength) {
                                maxLength = status[i].length;
                            }
                        }
                        for (var i = 0; i < status.length; ++i) {
                            var statusRadioDiv = dojo.create("span", {
                                'class': "annotation_info_editor_radio",
                                style: "width:" + (maxLength * 0.75) + "em;"
                            }, statusFlags);
                            var statusRadio = new dijitRadioButton({
                                value: status[i],
                                name: "status_" + uniqueName,
                                checked: status[i] == feature.status ? true : false
                            });
                            if (!hasWritePermission) {
                                statusRadio.set("disabled", true);
                            }
                            dojo.place(statusRadio.domNode, statusRadioDiv);
                            var statusLabel = dojo.create("label", {
                                innerHTML: status[i],
                                'class': "annotation_info_editor_radio_label"
                            }, statusRadioDiv);
                            statusRadios[status[i]] = statusRadio;
                            dojo.connect(statusRadio, "onMouseDown", function (div, radio, label) {
                                return function (event) {
                                    if (radio.checked) {
                                        deleteStatus();
                                        dojo.place(new dijitRadioButton({
                                            value: status[i],
                                            name: "status_" + uniqueName,
                                            checked: false
                                        }).domNode, radio.domNode, "replace");
                                    }
                                };
                            }(statusRadioDiv, statusRadio, statusLabel));
                            dojo.connect(statusRadio, "onChange", function (label) {
                                return function (selected) {
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

                var initDbxrefs = function (feature, config) {
                    if (config.hasDbxrefs) {
                        var oldDb;
                        var oldAccession;
                        var dbxrefs = new dojoItemFileWriteStore({
                            data: {
                                items: []
                            }
                        });
                        for (var i = 0; i < feature.dbxrefs.length; ++i) {
                            var dbxref = feature.dbxrefs[i];
                            if (dbxref.db != pubmedIdDb && dbxref.db != goIdDb) {
                                dbxrefs.newItem({db: dbxref.db, accession: dbxref.accession});
                            }
                        }
                        var dbxrefTableLayout = [{
                            cells: [
                                {
                                    name: 'DB',
                                    field: 'db',
                                    width: '40%',
                                    formatter: function (db) {
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
                                    formatter: function (accession) {
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

                        var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function () {
                            initTable(dbxrefTable.domNode, dbxrefsTable, dbxrefTable);
                            dojo.disconnect(handle);
                        });
                        if (reload) {
                            initTable(dbxrefTable.domNode, dbxrefsTable, dbxrefTable, timeout);
                        }


                        var dirty = false;
                        dojo.connect(dbxrefTable, "onStartEdit", function (inCell, inRowIndex) {
                            if (!dirty) {
                                oldDb = dbxrefTable.store.getValue(dbxrefTable.getItem(inRowIndex), "db");
                                oldAccession = dbxrefTable.store.getValue(dbxrefTable.getItem(inRowIndex), "accession");
                                dirty = true;
                            }
                        });

                        dojo.connect(dbxrefTable, "onCancelEdit", function (inRowIndex) {
                            dbxrefTable.store.setValue(dbxrefTable.getItem(inRowIndex), "db", oldDb);
                            dbxrefTable.store.setValue(dbxrefTable.getItem(inRowIndex), "accession", oldAccession);
                            dirty = false;
                        });

                        dojo.connect(dbxrefTable, "onApplyEdit", function (inRowIndex) {
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

                        dojo.connect(addDbxrefButton, "onclick", function () {
                            dbxrefTable.store.newItem({db: "", accession: ""});
                            dbxrefTable.scrollToRow(dbxrefTable.rowCount);
                        });

                        dojo.connect(deleteDbxrefButton, "onclick", function () {
                            var toBeDeleted = new Array();
                            var selected = dbxrefTable.selection.getSelected();
                            for (var i = 0; i < selected.length; ++i) {
                                var item = selected[i];
                                var db = dbxrefTable.store.getValue(item, "db");
                                var accession = dbxrefTable.store.getValue(item, "accession");
                                toBeDeleted.push({db: db, accession: accession});
                            }
                            dbxrefTable.removeSelectedRows();
                            deleteDbxrefs(toBeDeleted);
                        });
                    }
                    else {
                        dojo.style(dbxrefsDiv, "display", "none");
                    }
                };

                var initAttributes = function (feature, config) {
                    if (config.hasAttributes) {
                        var oldTag;
                        var oldValue;
                        var attributes = new dojoItemFileWriteStore({
                            data: {
                                items: []
                            }
                        });
                        for (var i = 0; i < feature.non_reserved_properties.length; ++i) {
                            var attribute = feature.non_reserved_properties[i];
                            attributes.newItem({tag: attribute.tag, value: attribute.value});
                        }
                        var attributeTableLayout = [{
                            cells: [
                                {
                                    name: 'Tag',
                                    field: 'tag',
                                    width: '40%',
                                    formatter: function (tag) {
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
                                    formatter: function (value) {
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

                        var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function () {
                            initTable(attributeTable.domNode, attributesTable, attributeTable);
                            dojo.disconnect(handle);
                        });
                        if (reload) {
                            initTable(attributeTable.domNode, attributesTable, attributeTable, timeout);
                        }

                        var dirty = false;

                        dojo.connect(attributeTable, "onStartEdit", function (inCell, inRowIndex) {
                            if (!dirty) {
                                oldTag = attributeTable.store.getValue(attributeTable.getItem(inRowIndex), "tag");
                                oldValue = attributeTable.store.getValue(attributeTable.getItem(inRowIndex), "value");
                                dirty = true;
                            }
                        });

                        dojo.connect(attributeTable, "onCancelEdit", function (inRowIndex) {
                            attributeTable.store.setValue(attributeTable.getItem(inRowIndex), "tag", oldTag);
                            attributeTable.store.setValue(attributeTable.getItem(inRowIndex), "value", oldValue);
                            dirty = false;
                        });

                        dojo.connect(attributeTable, "onApplyEdit", function (inRowIndex) {
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

                        dojo.connect(addAttributeButton, "onclick", function () {
                            attributeTable.store.newItem({tag: "", value: ""});
                            attributeTable.scrollToRow(attributeTable.rowCount);
                        });

                        dojo.connect(deleteAttributeButton, "onclick", function () {
                            var toBeDeleted = new Array();
                            var selected = attributeTable.selection.getSelected();
                            for (var i = 0; i < selected.length; ++i) {
                                var item = selected[i];
                                var tag = attributeTable.store.getValue(item, "tag");
                                var value = attributeTable.store.getValue(item, "value");
                                toBeDeleted.push({tag: tag, value: value});
                            }
                            attributeTable.removeSelectedRows();
                            deleteAttributes(toBeDeleted);
                        });
                    }
                    else {
                        dojo.style(attributesDiv, "display", "none");
                    }

                };

                var initPubmedIds = function (feature, config) {
                    if (config.hasPubmedIds) {
                        var oldPubmedId;
                        var pubmedIds = new dojoItemFileWriteStore({
                            data: {
                                items: []
                            }
                        });
                        for (var i = 0; i < feature.dbxrefs.length; ++i) {
                            var dbxref = feature.dbxrefs[i];
                            if (dbxref.db == pubmedIdDb) {
                                pubmedIds.newItem({pubmed_id: dbxref.accession});
                            }
                        }
                        var pubmedIdTableLayout = [{
                            cells: [
                                {
                                    name: 'Pubmed ID',
                                    field: 'pubmed_id',
                                    width: '100%',
                                    formatter: function (pubmedId) {
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

                        var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function () {
                            initTable(pubmedIdTable.domNode, pubmedIdsTable, pubmedIdTable);
                            dojo.disconnect(handle);
                        });
                        if (reload) {
                            initTable(pubmedIdTable.domNode, pubmedIdsTable, pubmedIdTable, timeout);
                        }

                        dojo.connect(pubmedIdTable, "onStartEdit", function (inCell, inRowIndex) {
                            oldPubmedId = pubmedIdTable.store.getValue(pubmedIdTable.getItem(inRowIndex), "pubmed_id");
                        });

                        dojo.connect(pubmedIdTable, "onApplyEdit", function (inRowIndex) {
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

                        dojo.connect(addPubmedIdButton, "onclick", function () {
                            pubmedIdTable.store.newItem({pubmed_id: ""});
                            pubmedIdTable.scrollToRow(pubmedIdTable.rowCount);
                        });

                        dojo.connect(deletePubmedIdButton, "onclick", function () {
                            var toBeDeleted = new Array();
                            var selected = pubmedIdTable.selection.getSelected();
                            for (var i = 0; i < selected.length; ++i) {
                                var item = selected[i];
                                var pubmedId = pubmedIdTable.store.getValue(item, "pubmed_id");
                                toBeDeleted.push({db: pubmedIdDb, accession: pubmedId});
                            }
                            pubmedIdTable.removeSelectedRows();
                            deletePubmedIds(toBeDeleted);
                        });
                    }
                    else {
                        dojo.style(pubmedIdsDiv, "display", "none");
                    }
                };

                var initGoIds = function (feature, config) {
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
                        for (var i = 0; i < feature.dbxrefs.length; ++i) {
                            var dbxref = feature.dbxrefs[i];
                            if (dbxref.db == goIdDb) {
                                goIds.newItem({go_id: goIdDb + ":" + dbxref.accession});
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
                                        createWidget: function (inNode, inDatum, inRowIndex) {
                                            var widget = new this.widgetClass(this.getWidgetProps(inDatum), inNode);
                                            var textBox = widget.domNode.childNodes[0].childNodes[0];
                                            dojo.connect(textBox, "onkeydown", function (event) {
                                                if (event.keyCode == dojo.keys.ENTER) {
                                                    if (dirty) {
                                                        dirty = false;
                                                        valid = validateGoId(textBox.value) ? true : false;
                                                    }
                                                }
                                            });
                                            var original = 'http://golr.geneontology.org/';
                                            //var original = 'http://golr.geneontology.org/solr/';
                                            //var original = 'http://golr.berkeleybop.org/solr/';
                                            var encoded_original = encodeURI(original);
                                            encoded_original = encoded_original.replace(/:/g,"%3A");
                                            encoded_original = encoded_original.replace(/\//g,"%2F");

                                            //var gserv = context_path + "/proxy/request/http/golr.geneontology.org%2Fsolr%2Fselect/json/";
                                            var gserv = context_path + "/proxy/request/"+encoded_original;
                                            var gconf = new bbop.golr.conf(amigo.data.golr);
                                            var args = {
                                                label_template: '{{annotation_class_label}} [{{annotation_class}}]',
                                                value_template: '{{annotation_class}}',
                                                list_select_callback: function (doc) {
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
                                    formatter: function (goId, rowIndex, cell) {
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

                        var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function () {
                            initTable(goIdTable.domNode, goIdsTable, goIdTable);
                            dojo.disconnect(handle);
                        });
                        if (reload) {
                            initTable(goIdTable.domNode, goIdsTable, goIdTable, timeout);
                        }

                        dojo.connect(goIdTable, "onStartEdit", function (inCell, inRowIndex) {
                            editingRow = inRowIndex;
                            oldGoId = goIdTable.store.getValue(goIdTable.getItem(inRowIndex), "go_id");
                            dirty = true;
                        });

                        // dojo.connect(goIdTable, "onApplyCellEdit", function(inValue, inRowIndex, inCellIndex) {
                        dojo.connect(goIdTable.store, "onSet", function (item, attribute, oldValue, newValue) {
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

                        dojo.connect(addGoIdButton, "onclick", function () {
                            goIdTable.store.newItem({go_id: ""});
                            goIdTable.scrollToRow(goIdTable.rowCount);
                        });

                        dojo.connect(deleteGoIdButton, "onclick", function () {
                            var toBeDeleted = new Array();
                            var selected = goIdTable.selection.getSelected();
                            for (var i = 0; i < selected.length; ++i) {
                                var item = selected[i];
                                var goId = goIdTable.store.getValue(item, "go_id");
                                toBeDeleted.push({db: goIdDb, accession: goId.substr(goIdDb.length + 1)});
                            }
                            goIdTable.removeSelectedRows();
                            deleteGoIds(toBeDeleted);
                        });
                    }
                    else {
                        dojo.style(goIdsDiv, "display", "none");
                    }
                };

                var initComments = function (feature, config) {
                    if (config.hasComments) {
                        var cannedComments = feature.canned_comments;
                        var oldComment;
                        var comments = new dojoItemFileWriteStore({
                            data: {
                                items: []
                            }
                        });
                        for (var i = 0; i < feature.comments.length; ++i) {
                            var comment = feature.comments[i];
                            comments.newItem({comment: comment});
                        }
                        var commentTableLayout = [{
                            cells: [
                                {
                                    name: 'Comment',
                                    field: 'comment',
                                    editable: hasWritePermission,
                                    type: dojox.grid.cells.ComboBox,
                                    options: cannedComments,
                                    formatter: function (comment) {
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

                        var handle = dojo.connect(AnnotTrack.popupDialog, "onFocus", function () {
                            initTable(commentTable.domNode, commentsTable, commentTable);
                            dojo.disconnect(handle);
                        });
                        if (reload) {
                            initTable(commentTable.domNode, commentsTable, commentTable, timeout);
                        }

                        dojo.connect(commentTable, "onStartEdit", function (inCell, inRowIndex) {
                            oldComment = commentTable.store.getValue(commentTable.getItem(inRowIndex), "comment");
                        });

                        dojo.connect(commentTable, "onApplyCellEdit", function (inValue, inRowIndex, inFieldIndex) {
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

                        dojo.connect(addCommentButton, "onclick", function () {
                            commentTable.store.newItem({comment: undefined});
                            commentTable.scrollToRow(commentTable.rowCount);
                        });

                        dojo.connect(deleteCommentButton, "onclick", function () {
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

                var processOtherMetadata = function () {
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
                            var statusRadioDiv = dojo.create("span", {
                                'class': "annotation_info_editor_radio",
                                style: "width:" + (maxLength * 0.75) + "em;"
                            }, statusFlags);
                            var statusRadio = new dijitRadioButton({
                                value: status[i],
                                name: "status_" + uniqueName
                            });
                            if (!hasWritePermission) {
                                statusRadio.set("disabled", true);
                            }
                            dojo.place(statusRadio.domNode, statusRadioDiv);
                            var statusLabel = dojo.create("label", {
                                innerHTML: status[i],
                                'class': "annotation_info_editor_radio_label"
                            }, statusRadioDiv);
                            statusRadios[status[i]] = statusRadio;
                            dojo.connect(statusRadio, "onMouseDown", function (div, radio, label) {
                                return function (event) {
                                    if (radio.checked) {
                                        deleteStatus();
                                        dojo.place(new dijitRadioButton({
                                            value: status[i],
                                            name: "status_" + uniqueName,
                                            checked: false
                                        }).domNode, radio.domNode, "replace");
                                    }
                                };
                            }(statusRadioDiv, statusRadio, statusLabel));
                            dojo.connect(statusRadio, "onChange", function (label) {
                                return function (selected) {
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

                var updateName = function (name) {
                    name = escapeString(name);
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "name": "' + name + '" } ]';
                    var operation = "set_name";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var updateSymbol = function (symbol) {
                    symbol = escapeString(symbol);
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "symbol": "' + symbol + '" } ]';
                    var operation = "set_symbol";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var updateDescription = function (description) {
                    description = escapeString(description);
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "description": "' + description + '" } ]';
                    var operation = "set_description";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var deleteStatus = function () {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "status": "' + status + '" } ]';
                    var operation = "delete_status";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var updateStatus = function (status) {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "status": "' + status + '" } ]';
                    var operation = "set_status";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var addDbxref = function (db, accession) {
                    db = escapeString(db);
                    accession = escapeString(accession);
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": [ { "db": "' + db + '", "accession": "' + accession + '" } ] } ]';
                    var operation = "add_non_primary_dbxrefs";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var deleteDbxrefs = function (dbxrefs) {
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

                var updateDbxref = function (oldDb, oldAccession, newDb, newAccession) {
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

                var addAttribute = function (tag, value) {
                    tag = escapeString(tag);
                    value = escapeString(value);
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "non_reserved_properties": [ { "tag": "' + tag + '", "value": "' + value + '" } ] } ]';
                    var operation = "add_non_reserved_properties";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var deleteAttributes = function (attributes) {
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

                var updateAttribute = function (oldTag, oldValue, newTag, newValue) {
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

                var confirmPubmedEntry = function (record) {
                    return confirm("Publication title: '" + record.PubmedArticleSet.PubmedArticle.MedlineCitation.Article.ArticleTitle + "'");
                };

                var addPubmedId = function (pubmedIdTable, row, pubmedId) {
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

                var deletePubmedIds = function (pubmedIds) {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": ' + JSON.stringify(pubmedIds) + ' } ]';
                    var operation = "delete_non_primary_dbxrefs";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var updatePubmedId = function (pubmedIdTable, row, oldPubmedId, newPubmedId) {
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

                var validateGoId = function (goId) {
                    var regex = new RegExp("^" + goIdDb + ":(\\d{7})$");
                    return regex.exec(goId);
                };

                var addGoId = function (goIdTable, row, goId, valid) {
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
                        goIdTable.close();
                    }
                };

                var deleteGoIds = function (goIds) {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "dbxrefs": ' + JSON.stringify(goIds) + ' } ]';
                    var operation = "delete_non_primary_dbxrefs";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

// var updateGoId = function(goIdTable, row, oldGoId, newGoId) {
                var updateGoId = function (goIdTable, item, oldGoId, newGoId, valid) {
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
                        goIdTable.close();
                    }
                };

                var addComment = function (comment) {
                    comment = escapeString(comment);
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "comments": [ "' + comment + '" ] } ]';
                    var operation = "add_comments";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var deleteComments = function (comments) {
                    for (var i = 0; i < comments.length; ++i) {
                        comments[i] = escapeString(comments[i]);
                    }
                    var features = '"features": [ { "uniquename": "' + uniqueName + '", "comments": ' + JSON.stringify(comments) + ' } ]';
                    var operation = "delete_comments";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    track.executeUpdateOperation(postData);
                    updateTimeLastUpdated();
                };

                var updateComment = function (oldComment, newComment) {
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

                var getCannedComments = function () {
                    var features = '"features": [ { "uniquename": "' + uniqueName + '" } ]';
                    var operation = "get_canned_comments";
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }';
                    dojo.xhrPost({
                        postData: postData,
                        url: context_path + "/AnnotationEditorService",
                        handleAs: "json",
                        sync: true,
                        timeout: 5000 * 1000, // Time in milliseconds
                        load: function (response, ioArgs) {
                            var feature = response.features[0];
                            cannedComments = feature.comments;
                        },
                        // The ERROR function will be called in an error case.
                        error: function (response, ioArgs) {
                            track.handleError(response);
                            console.error("HTTP status code: ", ioArgs.xhr.status);
                            return response;
                        }
                    });
                };

                init();
                return content;
            },

            undo: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.undoSelectedFeatures(selected);
            },

            undoSelectedFeatures: function (records) {
                var track = this;
                var uniqueNames = [];
                for (var i in records) {
                    var record = records[i];
                    var selfeat = record.feature;
                    var seltrack = record.track;
                    var topfeat = AnnotTrack.getTopLevelAnnotation(selfeat);
                    var uniqueName = topfeat.id();
                    // just checking to ensure that all features in selection are from
                    // this track
                    if (seltrack === track) {
                        var trackdiv = track.div;
                        var trackName = track.getUniqueTrackName();
                        uniqueNames.push(uniqueName);
                    }
                }
                this.undoFeaturesByUniqueName(uniqueNames, 1);
            },

            undoFeaturesByUniqueName: function (uniqueNames, count) {
                var track = this;
                var features = '"features": [';
                for (var i = 0; i < uniqueNames.length; ++i) {
                    var uniqueName = uniqueNames[i];
                    if (i > 0) {
                        features += ',';
                    }
                    features += ' { "uniquename": "' + uniqueName + '" } ';
                }
                features += ']';
                var operation = "undo";
                var trackName = this.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"' + (count ? ', "count": ' + count : '') + '}';
                this.executeUpdateOperation(postData, function (response) {
                    if (response && response.confirm) {
                        if (track.handleConfirm(response.confirm)) {
                            postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '", "confirm": true }';
                            track.executeUpdateOperation(postData);
                        }
                    }
                });
            },

            redo: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.redoSelectedFeatures(selected);
            },

            redoSelectedFeatures: function (records) {
                var track = this;
                var uniqueNames = [];
                var features = '"features": [';
                for (var i in records) {
                    var record = records[i];
                    var selfeat = record.feature;
                    var seltrack = record.track;
                    var topfeat = AnnotTrack.getTopLevelAnnotation(selfeat);
                    var uniqueName = topfeat.id();
                    // just checking to ensure that all features in selection are from
                    // this track
                    if (seltrack === track) {
                        uniqueNames.push(uniqueName);
                    }
                }
                this.redoFeaturesByUniqueName(uniqueNames, 1);
            },

            redoFeaturesByUniqueName: function (uniqueNames, count) {
                var features = '"features": [';
                for (var i = 0; i < uniqueNames.length; ++i) {
                    var uniqueName = uniqueNames[i];
                    if (i > 0) {
                        features += ',';
                    }
                    features += ' { "uniquename": "' + uniqueName + '" } ';
                }
                features += ']';
                var operation = "redo";
                var trackName = this.getUniqueTrackName();
                var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"' + (count ? ', "count": ' + count : '') + '}';
                this.executeUpdateOperation(postData);
            },

            getHistory: function () {
                var selected = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                this.getHistoryForSelectedFeatures(selected);
            },

            getHistoryForSelectedFeatures: function (selected) {
                var track = this;
                var content = dojo.create("div");
                var historyDiv = dojo.create("div", {className: "history_div"}, content);
                var historyTable = dojo.create("div", {className: "history_table"}, historyDiv);
                var historyHeader = dojo.create("div", {
                    className: "history_header",
                    innerHTML: "<span class='history_header_column history_column_operation history_column'>Operation</span><span class='history_header_column history_column'>Editor</span><span class='history_header_column history_column'>Date</span>"
                }, historyTable);
                var historyRows = dojo.create("div", {className: "history_rows"}, historyTable);
                var historyPreviewDiv = dojo.create("div", {className: "history_preview"}, historyDiv);
                var history;
                var selectedIndex = 0;
                var minFmin = undefined;
                var maxFmax = undefined;
                var current;
                var historyMenu;
                var canEdit = this.canEdit(selected[0].feature);

                function revert() {
                    if (selectedIndex == current) {
                        return;
                    }
                    if (selectedIndex < current) {
                        track.undoFeaturesByUniqueName([history[0].features[0].uniquename], current - selectedIndex);
                    }
                    else if (selectedIndex > current) {
                        track.redoFeaturesByUniqueName([history[0].features[0].uniquename], selectedIndex - current);
                    }
                    history[selectedIndex].current = true;
                    history[current].current = false;
                    dojo.attr(historyRows.childNodes.item(selectedIndex), "class", history[selectedIndex].current ? "history_row history_row_current" : "history_row");
                    dojo.attr(historyRows.childNodes.item(current), "class", "history_row");
                    current = selectedIndex;
                };

                function initMenu() {
                    historyMenu = new dijitMenu({});
                    historyMenu.addChild(new dijitMenuItem({
                        label: "Set as current",
                        onClick: function () {
                            revert();
                        }
                    }));
                    historyMenu.startup();
                }

                var cleanupDiv = function (div) {
                    if (div.style.top) {
                        div.style.top = null;
                    }
                    if (div.style.visibility) {
                        div.style.visibility = null;
                    }
// annot_context_menu.unBindDomNode(div);
                    $(div).unbind();
                    for (var i = 0; i < div.childNodes.length; ++i) {
                        cleanupDiv(div.childNodes[i]);
                    }
                };

                var displayPreview = function (index) {
                    var historyItem = history[index];
                    var afeature = historyItem.features[0];
                    var jfeature = JSONUtils.createJBrowseFeature(afeature);
                    var fmin = afeature.location.fmin;
                    var fmax = afeature.location.fmax;
                    var maxLength = maxFmax - minFmin;
// track.featureStore._add_getters(track.attrs.accessors().get, jfeature);
                    historyPreviewDiv.featureLayout = new Layout(fmin, fmax);
                    historyPreviewDiv.featureNodes = new Array();
                    historyPreviewDiv.startBase = minFmin - (maxLength * 0.1);
                    historyPreviewDiv.endBase = maxFmax + (maxLength * 0.1);
                    var coords = dojo.position(historyPreviewDiv);
                    // setting labelScale and descriptionScale parameter to 100 px/bp,
                    // so neither should get triggered
                    var featDiv = track.renderFeature(jfeature, jfeature.uid, historyPreviewDiv, coords.w / (maxLength), 100, 100, minFmin, maxFmax, true);
                    cleanupDiv(featDiv);

                    historyMenu.bindDomNode(featDiv);

                    while (historyPreviewDiv.hasChildNodes()) {
                        historyPreviewDiv.removeChild(historyPreviewDiv.lastChild);
                    }
                    historyPreviewDiv.appendChild(featDiv);
                    dojo.attr(historyRows.childNodes.item(selectedIndex), "class", history[selectedIndex].current ? "history_row history_row_current" : "history_row");
                    dojo.attr(historyRows.childNodes.item(index), "class", "history_row history_row_selected");
                    selectedIndex = index;
                };

                var displayHistory = function () {
                    for (var i = 0; i < history.length; ++i) {
                        var historyItem = history[i];
                        var rowCssClass = "history_row";
                        var row = dojo.create("div", {className: rowCssClass}, historyRows);
                        var columnCssClass = "history_column";
                        dojo.create("span", {
                            className: columnCssClass + " history_column_operation ",
                            innerHTML: historyItem.operation
                        }, row);
                        dojo.create("span", {className: columnCssClass, innerHTML: historyItem.editor}, row);
                        dojo.create("span", {
                            className: columnCssClass + " history_column_date",
                            innerHTML: historyItem.date
                        }, row);
                        var revertButton = new dijitButton({
                            label: "Revert",
                            showLabel: false,
                            iconClass: "dijitIconUndo",
                            'class': "revert_button",
                            onClick: function (index) {
                                return function () {
                                    selectedIndex = index;
                                    revert();
                                }
                            }(i)
                        });
                        if (!canEdit) {
                            revertButton.set("disabled", true);
                        }
                        dojo.place(revertButton.domNode, row);
                        var afeature = historyItem.features[0];
                        var fmin = afeature.location.fmin;
                        var fmax = afeature.location.fmax;
                        if (minFmin == undefined || fmin < minFmin) {
                            minFmin = fmin;
                        }
                        if (maxFmax == undefined || fmax > maxFmax) {
                            maxFmax = fmax;
                        }

                        if (historyItem.current) {
                            current = i;
                        }

                        dojo.connect(row, "onclick", row, function (index) {
                            return function () {
                                displayPreview(index);
                            };
                        }(i));

                        dojo.connect(row, "oncontextmenu", row, function (index) {
                            return function () {
                                displayPreview(index);
                            };
                        }(i));

                        historyMenu.bindDomNode(row);

                    }
                    displayPreview(current);
                    var coords = dojo.position(row);
                    historyRows.scrollTop = selectedIndex * coords.h;
                };

                var fetchHistory = function () {
                    var features = '"features": [';
                    for (var i in selected) {
                        var record = selected[i];
                        var annot = AnnotTrack.getTopLevelAnnotation(record.feature);
                        var uniqueName = annot.id();
                        // just checking to ensure that all features in selection are
                        // from this track
                        if (record.track === track) {
                            var trackdiv = track.div;
                            var trackName = track.getUniqueTrackName();

                            if (i > 0) {
                                features += ',';
                            }
                            features += ' { "uniquename": "' + uniqueName + '" } ';
                        }
                    }
                    features += ']';
                    var operation = "get_history_for_features";
                    var trackName = track.getUniqueTrackName();
                    dojo.xhrPost({
                        postData: '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }',
                        url: context_path + "/AnnotationEditorService",
                        handleAs: "json",
                        timeout: 5000 * 1000, // Time in milliseconds
                        load: function (response, ioArgs) {
                            var features = response.features;
// for (var i = 0; i < features.length; ++i) {
// displayHistory(features[i].history);
// }
                            history = features[i].history;
                            displayHistory();
                        },
                        // The ERROR function will be called in an error case.
                        error: function (response, ioArgs) { //
                            track.handleError(response);
                            return response; //
                        }

                    });
                };

                initMenu();
                fetchHistory();
                this.openDialog("History", content);
                AnnotTrack.popupDialog.resize();
                AnnotTrack.popupDialog._position();
// this.popupDialog.hide();
// this.openDialog("History", content);
            },

            getAnnotationInformation: function () {
                var selected = this.selectionManager.getSelection();
                this.getInformationForSelectedAnnotations(selected);
            },

            getInformationForSelectedAnnotations: function (records) {
                var track = this;
                var features = '"features": [';
                var seqtrack = track.getSequenceTrack();
                for (var i in records) {
                    var record = records[i];
                    var selfeat = record.feature;
                    var seltrack = record.track;
                    var topfeat = AnnotTrack.getTopLevelAnnotation(selfeat);
                    var uniqueName = topfeat.id();
                    // just checking to ensure that all features in selection are from
                    // this annotation track
                    // (or from sequence annotation track);
                    if (seltrack === track || (seqtrack && (seltrack === seqtrack))) {
                        var trackdiv = track.div;
                        var trackName = track.getUniqueTrackName();

                        if (i > 0) {
                            features += ',';
                        }
                        features += ' { "uniquename": "' + uniqueName + '" } ';
                    }
                }
                features += ']';
                var operation = "get_information";
                var trackName = track.getUniqueTrackName();
                var information = "";
                dojo.xhrPost({
                    postData: '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '" }',
                    url: context_path + "/AnnotationEditorService",
                    handleAs: "json",
                    timeout: 5000 * 1000, // Time in milliseconds
                    load: function (response, ioArgs) {
                        for (var i = 0; i < response.features.length; ++i) {
                            var feature = response.features[i];
                            if (i > 0) {
                                information += "<hr/>";
                            }
                            information += "Unique id: " + feature.uniquename + "<br/>";
                            information += "Date of creation: " + feature.time_accessioned + "<br/>";
                            information += "Owner: " + feature.owner + "<br/>";
                            if (feature.parent_ids) {
                                information += "Parent ids: " + feature.parent_ids + "<br/>";
                            }
                        }
                        track.openDialog("Annotation information", information);
                    },
                    // The ERROR function will be called in an error case.
                    error: function (response, ioArgs) {
                        track.handleError(response);
                        console.log("Annotation server error--maybe you forgot to login to the server?");
                        console.error("HTTP status code: ", ioArgs.xhr.status);
                        //
                        // dojo.byId("replace").innerHTML = 'Loading the resource
                        // from the server did not work';
                        return response;
                    }
                });
            },

            getGff3: function () {
                var selected = this.selectionManager.getSelection();
                this.getGff3ForSelectedFeatures(selected);
            },

            getGff3ForSelectedFeatures: function (records) {
                var track = this;

                var content = dojo.create("div", {className: "get_gff3"});
                var textArea = dojo.create("textarea", {className: "gff3_area", readonly: true}, content);

                var fetchGff3 = function () {
                    var features = '"features": [';
                    for (var i = 0; i < records.length; ++i) {
                        var record = records[i];
                        var annot = record.feature;
                        var seltrack = record.track;
                        var uniqueName = annot.getUniqueName();
                        // just checking to ensure that all features in selection are
                        // from this track
                        if (seltrack === track) {
                            var trackdiv = track.div;
                            var trackName = track.getUniqueTrackName();

                            if (i > 0) {
                                features += ',';
                            }
                            features += ' { "uniquename": "' + uniqueName + '" } ';
                        }
                    }
                    features += ']';
                    var operation = "get_gff3";
                    var trackName = track.getUniqueTrackName();
                    var postData = '{ "track": "' + trackName + '", ' + features + ', "operation": "' + operation + '"';
                    postData += ' }';
                    dojo.xhrPost({
                        postData: postData,
                        url: context_path + "/AnnotationEditorService",
                        handleAs: "text",
                        timeout: 5000 * 1000, // Time in milliseconds
                        load: function (response, ioArgs) {
                            var textAreaContent = response;
                            dojo.attr(textArea, "innerHTML", textAreaContent);
                        },
                        // The ERROR function will be called in an error case.
                        error: function (response, ioArgs) {
                            track.handleError(response);
                            console.log(response);
                            console.log("Annotation server error--maybe you forgot to login to the server?");
                            console.error("HTTP status code: ", ioArgs.xhr.status);
                            //
                            // dojo.byId("replace").innerHTML = 'Loading the
                            // resourcgetFeaturee from the server did not work';
                            return response;
                        }

                    });
                };
                fetchGff3(records);

                this.openDialog("GFF3", content);
            },

            getSequence: function () {
                var selected = this.selectionManager.getSelection();
                this.getSequenceForSelectedFeatures(selected);
            },

            getSequenceForSelectedFeatures: function (records) {
                var track = this;

                var content = dojo.create("div", {className: "get_sequence"});
                var textArea = dojo.create("textarea", {className: "sequence_area", readonly: true}, content);
                var form = dojo.create("form", {}, content);
                var peptideButtonDiv = dojo.create("div", {className: "first_button_div"}, form);
                var peptideButton = dojo.create("input", {
                    type: "radio",
                    name: "type",
                    checked: true
                }, peptideButtonDiv);
                var peptideButtonLabel = dojo.create("label", {
                    innerHTML: "Peptide sequence",
                    className: "button_label"
                }, peptideButtonDiv);
                var cdnaButtonDiv = dojo.create("div", {className: "button_div"}, form);
                var cdnaButton = dojo.create("input", {type: "radio", name: "type"}, cdnaButtonDiv);
                var cdnaButtonLabel = dojo.create("label", {
                    innerHTML: "cDNA sequence",
                    className: "button_label"
                }, cdnaButtonDiv);
                var cdsButtonDiv = dojo.create("div", {className: "button_div"}, form);
                var cdsButton = dojo.create("input", {type: "radio", name: "type"}, cdsButtonDiv);
                var cdsButtonLabel = dojo.create("label", {
                    innerHTML: "CDS sequence",
                    className: "button_label"
                }, cdsButtonDiv);
                var genomicButtonDiv = dojo.create("div", {className: "button_div"}, form);
                var genomicButton = dojo.create("input", {type: "radio", name: "type"}, genomicButtonDiv);
                var genomicButtonLabel = dojo.create("label", {
                    innerHTML: "Genomic sequence",
                    className: "button_label"
                }, genomicButtonDiv);
                var genomicWithFlankButtonDiv = dojo.create("div", {className: "button_div"}, form);
                var genomicWithFlankButton = dojo.create("input", {
                    type: "radio",
                    name: "type"
                }, genomicWithFlankButtonDiv);
                var genomicWithFlankButtonLabel = dojo.create("label", {
                    innerHTML: "Genomic sequence +/-",
                    className: "button_label"
                }, genomicWithFlankButtonDiv);
                var genomicWithFlankField = dojo.create("input", {
                    type: "text",
                    size: 5,
                    className: "button_field",
                    value: "500"
                }, genomicWithFlankButtonDiv);
                var genomicWithFlankFieldLabel = dojo.create("label", {
                    innerHTML: "bases",
                    className: "button_label"
                }, genomicWithFlankButtonDiv);

                var fetchSequence = function (type) {
                    var features = '"features": [';
                    for (var i = 0; i < records.length; ++i) {
                        var record = records[i];
                        var annot = record.feature;
                        var seltrack = record.track;
                        var uniqueName = annot.getUniqueName();
                        // just checking to ensure that all features in selection are
                        // from this track
                        if (seltrack === track) {
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
                    dojo.xhrPost({
                        postData: postData,
                        url: context_path + "/AnnotationEditorService",
                        handleAs: "json",
                        timeout: 5000 * 1000, // Time in milliseconds
                        load: function (response, ioArgs) {
                            var textAreaContent = "";
                            for (var i = 0; i < response.features.length; ++i) {
                                var feature = response.features[i];
                                var cvterm = feature.type;
                                var residues = feature.residues;
                                var loc = feature.location;
                                textAreaContent += "&gt;" + feature.uniquename + " (" + cvterm.cv.name + ":" + cvterm.name + ") " + residues.length + " residues [" + track.refSeq.name + ":" + (loc.fmin + 1) + "-" + loc.fmax + " " + (loc.strand == -1 ? "-" : loc.strand == 1 ? "+" : "no") + " strand] [" + type + (flank > 0 ? " +/- " + flank + " bases" : "") + "]\n";
                                var lineLength = 70;
                                for (var j = 0; j < residues.length; j += lineLength) {
                                    textAreaContent += residues.substr(j, lineLength) + "\n";
                                }
                            }
                            dojo.attr(textArea, "innerHTML", textAreaContent);
                        },
                        // The ERROR function will be called in an error case.
                        error: function (response, ioArgs) {
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
                var callback = function (event) {
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
            },

            searchSequence: function () {
                var track = this;
                var starts = new Object();
                var browser = track.gview.browser;
                for (i in browser.allRefs) {
                    var refSeq = browser.allRefs[i];
                    starts[refSeq.name] = refSeq.start;
                }
                var search = new SequenceSearch(context_path);
                search.setRedirectCallback(function (id, fmin, fmax) {
                    var loc = id + ":" + fmin + "-" + fmax;
                    var locobj = {
                        ref: id,
                        start: fmin,
                        end: fmax
                    };
                    if (id == track.refSeq.name) {
                        // track.gview.browser.navigateTo(loc);
                        var highlightSearchedRegions = track.gview.browser.config.highlightSearchedRegions;
                        track.gview.browser.config.highlightSearchedRegions = true;
                        track.gview.browser.showRegionWithHighlight(locobj);
                        track.gview.browser.config.highlightSearchedRegions = highlightSearchedRegions;
                        // AnnotTrack.popupDialog.hide();
                    }
                    else {
                        // var url = window.location.toString().replace(/loc=.+/, "loc=" +
                        // loc);
                        // window.location.replace(url);
                        var highlightSearchedRegions = track.gview.browser.config.highlightSearchedRegions;
                        track.gview.browser.config.highlightSearchedRegions = true;
                        track.gview.browser.showRegionWithHighlight(locobj);
                        track.gview.browser.config.highlightSearchedRegions = highlightSearchedRegions;
                        // AnnotTrack.popupDialog.hide();
                    }
                });
                search.setErrorCallback(function (response) {
                    track.handleError(response);
                });
                var content = search.searchSequence(track.getUniqueTrackName(), track.refSeq.name, starts);
                if (content) {
                    this.openDialog("Search sequence", content);
                }
            },

            exportData: function (key, options) {
                var track = this;
                var adapter = key;
                var content = dojo.create("div");
                var waitingDiv = dojo.create("div", {innerHTML: "<img class='waiting_image' src='plugins/WebApollo/img/loading.gif' />"}, content);
                var responseDiv = dojo.create("div", {className: "export_response"}, content);

                dojo.xhrGet({
                    url: context_path + "/IOService?operation=write&adapter=" + adapter + "&sequences=" + track.getUniqueTrackName() + "&" + options,
                    handleAs: "json",
                    load: function (response, ioArgs) {
                        dojo.create("a", {
                            innerHTML: response.filename,
                            href: context_path + "/IOService/download?uuid=" + response.uuid + "&exportType=" + response.exportType + "&seqType=" + response.sequenceType + "&format=" + response.format
                        }, content);
                        dojo.style(waitingDiv, {display: "none"});
                    },
                    // The ERROR function will be called in an error case.
                    error: function (response, ioArgs) {
                        dojo.style(waitingDiv, {display: "none"});
                        responseDiv.innerHTML = "Unable to export data";
                        track.handleError(response);
                    }
                });
                track.openDialog("Export " + key, content);
            },

            zoomToBaseLevel: function (event) {
                var coordinate = this.getGenomeCoord(event);
                this.gview.zoomToBaseLevel(event, coordinate);
            },


            scrollToNextEdge: function (event) {
                // var coordinate = this.getGenomeCoord(event);
                var track = this;
                var vregion = this.gview.visibleRegion();
                var coordinate = (vregion.start + vregion.end) / 2;
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
                                track.selectionManager.addToSelection({feature: subfeats[i], track: track});
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
                    else {
                        var childfeats = selfeat.children();
                        for (var i = 0; i < childfeats.length; i++) {
                            var cfeat = childfeats[i];
                            var cmin = cfeat.get('start');
                            var cmax = cfeat.get('end');
                            // if (cmin > coordinate) {
                            if ((cmin - coordinate) > 10) { // fuzz factor of 10 bases
                                coordDelta = Math.min(coordDelta, cmin - coordinate);
                            }
                            // if (cmax > coordinate) {
                            if ((cmax - coordinate) > 10) { // fuzz factor of 10 bases
                                coordDelta = Math.min(coordDelta, cmax - coordinate);
                            }
                        }
                        // find closest edge right of current coord
                        if (coordDelta != Number.MAX_VALUE) {
                            var newCenter = coordinate + coordDelta;
                            centerAtBase(newCenter);
                        }
                    }
                }
            },

            scrollToPreviousEdge: function (event) {
                // var coordinate = this.getGenomeCoord(event);
                var track = this;
                var vregion = this.gview.visibleRegion();
                var coordinate = (vregion.start + vregion.end) / 2;
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
                                track.selectionManager.addToSelection({feature: subfeats[i], track: track});
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
                    else {
                        var childfeats = selfeat.children();
                        for (var i = 0; i < childfeats.length; i++) {
                            var cfeat = childfeats[i];
                            var cmin = cfeat.get('start');
                            var cmax = cfeat.get('end');
                            // if (cmin > coordinate) {
                            if ((coordinate - cmin) > 10) { // fuzz factor of 10 bases
                                coordDelta = Math.min(coordDelta, coordinate - cmin);
                            }
                            // if (cmax > coordinate) {
                            if ((coordinate - cmax) > 10) { // fuzz factor of 10 bases
                                coordDelta = Math.min(coordDelta, coordinate - cmax);
                            }
                        }
                        // find closest edge right of current coord
                        if (coordDelta != Number.MAX_VALUE) {
                            var newCenter = coordinate - coordDelta;
                            centerAtBase(newCenter);
                        }
                    }
                }
            },

            scrollToNextTopLevelFeature: function () {
                var selected = this.selectionManager.getSelection();
                if (!selected || !selected.length) {
                    return;
                }
                var features = [];
                for (var i in this.store.features) {
                    features.push(this.store.features[i]);
                }
                this.sortAnnotationsByLocation(features);
                var idx = this.binarySearch(features, AnnotTrack.getTopLevelAnnotation(selected[0].feature));
                if (idx < 0 || idx >= features.length - 1) {
                    return;
                }
                this.gview.centerAtBase(features[idx + 1].get("start"));
                this.selectionManager.removeFromSelection({feature: selected[0].feature, track: this});
                this.selectionManager.addToSelection({feature: features[idx + 1], track: this});
            },

            scrollToPreviousTopLevelFeature: function () {
                var selected = this.selectionManager.getSelection();
                if (!selected || !selected.length) {
                    return;
                }
                var features = [];
                for (var i in this.store.features) {
                    features.push(this.store.features[i]);
                }
                this.sortAnnotationsByLocation(features);
                var idx = this.binarySearch(features, AnnotTrack.getTopLevelAnnotation(selected[0].feature));
                if (idx <= 0 || idx > features.length - 1) {
                    return;
                }
                this.gview.centerAtBase(features[idx - 1].get("end"));
                this.selectionManager.removeFromSelection({feature: selected[0].feature, track: this});
                this.selectionManager.addToSelection({feature: features[idx - 1], track: this});
            },

            binarySearch: function (features, feature) {
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

            zoomBackOut: function (event) {
                this.gview.zoomBackOut(event);
            },

            handleError: function (response) {
                console.log("ERROR: ");
                console.log(response);  // in Firebug, allows retrieval of stack trace,
                                        // jump to code, etc.
                var error = response.responseText && response.responseText.match("^<") != "<" ? JSON.parse(response.responseText) : response.response.data;
                if (error && error.error) {
                    alert(error.error);
                    console.log(error.error);
                    return false;
                }
            },

            handleConfirm: function (response) {
                return confirm(response);
            },

            logout: function () {
                dojo.xhrPost({
                    url: context_path + "/Login?operation=logout",
                    handleAs: "json",
                    timeout: 5 * 1000, // Time in milliseconds
                    // The LOAD function will be called on a successful response.
                    load: function (response, ioArgs) { //
                        if (window.parent) window.parent.location.reload();
                        else window.location.reload();
                    },
                    error: function (response, ioArgs) { //
                        alert('Failed to log out cleanly.  Please refresh your browser.');
                    }
                });
            },

            login: function () {
                var track = this;
                dojo.xhrGet({
                    url: context_path + "/Login",
                    handleAs: "text",
                    timeout: 5 * 60,
                    load: function (response, ioArgs) {
                        var dialog = new dojoxDialogSimple({
                            preventCache: true,
                            refreshOnShow: true,
                            executeScripts: true
                        });
                        if (track.browser.config.disableJBrowseMode) {
                            dialog.hide = function () {
                            };
                        }
                        dialog.startup();
                        dialog.set("title", "Login");
                        dialog.set("content", response);
                        dialog.show();
                    }
                });
            },

            initLoginMenu: function () {
                var track = this;
                var browser = this.gview.browser;
                if (this.permission) {   // permission only set if permission request
                    // succeeded
                    browser.addGlobalMenuItem('user',
                        new dijitMenuItem({
                            label: 'Logout',
                            onClick: function () {
                                console.log("clicked stub for logging out");
                                // attempted to do
                                // client-side session
                                // cookie deletion, but
                                // doesn't
                                // work because JSESSIONID
                                // is flagged as "HttpOnly"
                                // document.cookie =
                                // "JSESSIONID=;
                                // path=/ApolloWeb/";

                                // reload page after
                                // removing session cookie?
                                dojo.xhrPost({
                                    url: context_path + "/Login?operation=logout",
                                    handleAs: "json",
                                    timeout: 5 * 1000, // Time
                                    // in
                                    // milliseconds
                                    // The LOAD function
                                    // will be called on a
                                    // successful response.
                                    load: function (response, ioArgs) { //
                                        if (window.parent) window.parent.location.reload();
                                        else window.location.reload();
                                    },
                                    error: function (response, ioArgs) { //
                                        alert('Failed to log out cleanly.  Please refresh your browser.');
                                    }
                                });
                            }
                        })
                    );
                    var userMenu = browser.makeGlobalMenu('user');
                    loginButton = new dijitDropDownButton(
                        {
                            className: 'user',
                            innerHTML: '<span class="usericon"></span>' + this.username,
                            title: 'user logged in: UserName',
                            dropDown: userMenu
                        });
                }
                else {
                    loginButton = new dijitButton(
                        {
                            className: 'login',
                            innerHTML: "Login",
                            onClick: function () {
                                dojo.xhrGet({
                                    url: context_path + "/Login?operation=login",
                                    handleAs: "text",
                                    timeout: 5 * 60,
                                    load: function (response, ioArgs) {
                                        track.openDialog("Login", response);
                                    }
                                });
                            }
                        });
                }
                browser.afterMilestone('initView', function () {
                    // must append after menubar is created, plugin constructor called
                    // before menubar exists,
                    // browser.initView called after menubar exists
                    browser.menuBar.appendChild(loginButton.domNode);
                });
            },

            initAnnotContextMenu: function () {
                var thisB = this;
                contextMenuItems = new Array();
                annot_context_menu = new dijit.Menu({});
                var permission = thisB.permission;
                var index = 0;
                annot_context_menu.addChild(new dijit.MenuItem({
                    label: "Get Sequence",
                    onClick: function (event) {
                        thisB.getSequence();
                    }
                }));
                contextMenuItems["get_sequence"] = index++;

                annot_context_menu.addChild(new dijit.MenuItem({
                    label: "Get GFF3",
                    onClick: function (event) {
                        thisB.getGff3();
                    }
                }));
                contextMenuItems["get_gff3"] = index++;

                annot_context_menu.addChild(new dijit.MenuItem({
                    label: "Zoom to Base Level",
                    onClick: function (event) {
                        if (thisB.getMenuItem("zoom_to_base_level").get("label") == "Zoom to Base Level") {
                            thisB.zoomToBaseLevel(thisB.annot_context_mousedown);
                        }
                        else {
                            thisB.zoomBackOut(thisB.annot_context_mousedown);
                        }
                    }
                }));
                contextMenuItems["zoom_to_base_level"] = index++;
                if (!(permission & Permission.WRITE)) {
                    annot_context_menu.addChild(new dijit.MenuSeparator());
                    index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Information Editor (alt-click)",
                        onClick: function (event) {
                            thisB.getAnnotationInfoEditor();
                        }
                    }));
                    contextMenuItems["annotation_info_editor"] = index++;
                }
                if (permission & Permission.WRITE) {
                    //annot_context_menu.addChild(new dijit.MenuSeparator());
                    //index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Edit Information (alt-click)",
                        onClick: function (event) {
                            thisB.getAnnotationInfoEditor();
                        }
                    }));
                    contextMenuItems["annotation_info_editor"] = index++;
                    annot_context_menu.addChild(new dijit.MenuSeparator());
                    index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Delete",
                        onClick: function () {
                            thisB.deleteSelectedFeatures();
                        }
                    }));
                    contextMenuItems["delete"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Merge",
                        onClick: function () {
                            thisB.mergeSelectedFeatures();
                        }
                    }));
                    contextMenuItems["merge"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Split",
                        onClick: function (event) {
                            // use annot_context_mousedown instead of current event, since
                            // want to split
                            // at mouse position of event that triggered annot_context_menu
                            // popup
                            thisB.splitSelectedFeatures(thisB.annot_context_mousedown);
                        }
                    }));
                    contextMenuItems["split"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Duplicate",
                        onClick: function (event) {
                            // use annot_context_mousedown instead of current event, since
                            // want to split
                            // at mouse position of event that triggered annot_context_menu
                            // popup
                            thisB.duplicateSelectedFeatures(thisB.annot_context_mousedown);
                        }
                    }));
                    contextMenuItems["duplicate"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Make Intron",
                        // use annot_context_mousedown instead of current event, since want
                        // to split
                        // at mouse position of event that triggered annot_context_menu
                        // popup
                        onClick: function (event) {
                            thisB.makeIntron(thisB.annot_context_mousedown);
                        }
                    }));
                    contextMenuItems["make_intron"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Move to Opposite Strand",
                        onClick: function (event) {
                            thisB.flipStrand();
                        }
                    }));
                    contextMenuItems["flip_strand"] = index++;
                    annot_context_menu.addChild(new dijit.MenuSeparator());
                    index++;

                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set Translation Start",
                        // use annot_context_mousedown instead of current event, since want
                        // to split
                        // at mouse position of event that triggered annot_context_menu
                        // popup
                        onClick: function (event) {
                            thisB.setTranslationStart(thisB.annot_context_mousedown);
                        }
                    }));
                    contextMenuItems["set_translation_start"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set Translation End",
                        // use annot_context_mousedown instead of current event, since want
                        // to split
                        // at mouse position of event that triggered annot_context_menu
                        // popup
                        onClick: function (event) {
                            thisB.setTranslationEnd(thisB.annot_context_mousedown);
                        }
                    }));
                    contextMenuItems["set_translation_end"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set Longest ORF",
                        // use annot_context_mousedown instead of current event, since want
                        // to split
                        // at mouse position of event that triggered annot_context_menu
                        // popup
                        onClick: function (event) {
                            thisB.setLongestORF();
                        }
                    }));
                    contextMenuItems["set_longest_orf"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set Readthrough Stop Codon",
                        onClick: function (event) {
                            thisB.setReadthroughStopCodon();
                        }
                    }));
                    contextMenuItems["set_readthrough_stop_codon"] = index++;
                    annot_context_menu.addChild(new dijit.MenuSeparator());
                    index++;

                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set as 5' end",
                        onClick: function (event) {
                            thisB.setAsFivePrimeEnd();
                        }
                    }));
                    contextMenuItems["set_as_five_prime_end"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set as 3' End",
                        onClick: function (event) {
                            thisB.setAsThreePrimeEnd();
                        }
                    }));
                    contextMenuItems["set_as_three_prime_end"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set both Ends",
                        onClick: function (event) {
                            thisB.setBothEnds();
                        }
                    }));
                    contextMenuItems["set_both_ends"] = index++;
                    annot_context_menu.addChild(new dijit.MenuSeparator());
                    index++;
                    contextMenuItems["set_downstream_donor"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set to Downstream Splice Donor",
                        onClick: function (event) {
                            thisB.setToDownstreamDonor();
                        }
                    }));
                    contextMenuItems["set_upstream_donor"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set to Upstream Splice Donor",
                        onClick: function (event) {
                            thisB.setToUpstreamDonor();
                        }
                    }));
                    contextMenuItems["set_downstream_acceptor"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set to Downstream Splice Acceptor",
                        onClick: function (event) {
                            thisB.setToDownstreamAcceptor();
                        }
                    }));
                    contextMenuItems["set_upstream_acceptor"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Set to Upstream Splice Acceptor",
                        onClick: function (event) {
                            thisB.setToUpstreamAcceptor();
                        }
                    }));
                    annot_context_menu.addChild(new dijit.MenuSeparator());
                    index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Undo",
                        onClick: function (event) {
                            thisB.undo();
                        }
                    }));
                    contextMenuItems["undo"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Redo",
                        onClick: function (event) {
                            thisB.redo();
                        }
                    }));
                    contextMenuItems["redo"] = index++;
                    annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Show History",
                        onClick: function (event) {
                            thisB.getHistory();
                        }
                    }));
                    contextMenuItems["history"] = index++;
                }

                annot_context_menu.onOpen = function (event) {
                    // keeping track of mousedown event that triggered annot_context_menu popup
                    // because need mouse position of that event for some actions
                    thisB.annot_context_mousedown = thisB.last_mousedown_event;
                    if (thisB.permission & Permission.WRITE) {
                        thisB.updateMenu();
                    }
                    dojo.forEach(this.getChildren(), function (item, idx, arr) {
                        if (item instanceof dijit.MenuItem) {
                            item._setSelected(false);
                            // check for _onUnhover, since latest
                            // dijit.MenuItem does not have _onUnhover()
                            // method
                            if (item._onUnhover) {
                                item._onUnhover();
                            }
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
            initSaveMenu: function () {
                var track = this;
                dojo.xhrPost({
                    sync: true,
                    postData: '{ "track": "' + track.getUniqueTrackName() + '", "operation": "get_data_adapters" }',
                    url: context_path + "/AnnotationEditorService",
                    handleAs: "json",
                    timeout: 5 * 1000, // Time in milliseconds
                    // The LOAD function will be called on a successful response.
                    load: function (response, ioArgs) { //
                        var dataAdapters = response.data_adapters;
                        for (var i = 0; i < dataAdapters.length; ++i) {
                            var dataAdapter = dataAdapters[i];
                            if (track.permission & dataAdapter.permission) {
                                track.exportAdapters.push(dataAdapter);
                            }
                        }
                        // remake track label pulldown menu so will include
                        // dataAdapter submenu
                        track.makeTrackMenu();
                    },
                    error: function (response, ioArgs) { //
                    }
                });
            },

            makeTrackMenu: function () {
                this.inherited(arguments);
                var track = this;
                var options = this._trackMenuOptions();
                if (options && options.length && this.label && this.labelMenuButton && this.exportAdapters.length > 0) {
                    var dataAdaptersMenu = new dijit.Menu();
                    for (var i = 0; i < this.exportAdapters.length; i++) {
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
                                submenu.addChild(new dijit.MenuItem({
                                    label: subAdapter.key,
                                    onClick: function (key, options) {
                                        return function () {
                                            track.exportData(key, options);
                                        };
                                    }(subAdapter.key, subAdapter.options)
                                }));
                            }
                        }
                        else {
                            dataAdaptersMenu.addChild(new dijit.MenuItem({
                                label: dataAdapter.key,
                                onClick: function (key, options) {
                                    return function () {
                                        track.exportData(key, options);
                                    };
                                }(dataAdapter.key, dataAdapter.options)
                            }));
                        }
                    }
                    // if there's a menu separator, add right before first seperator (which
                    // is where default save is added),
                    // otherwise add at end
                    var mitems = this.trackMenu.getChildren();
                    for (var mindex = 0; mindex < mitems.length; mindex++) {
                        if (mitems[mindex].type == "dijit/MenuSeparator") {
                            break;
                        }
                    }

                    var savePopup = new dijit.PopupMenuItem({
                        label: "Save track data",
                        iconClass: 'dijitIconSave',
                        popup: dataAdaptersMenu
                    });
                    this.trackMenu.addChild(savePopup, mindex);
                }
            },

            getPermission: function (callback) {
                var thisB = this;
                var loadCallback = callback;
                var success = true;
                dojo.xhrPost({
                    sync: true,
                    postData: '{ "track": "' + thisB.getUniqueTrackName() + '", "operation": "get_user_permission" }',
                    url: context_path + "/AnnotationEditorService",
                    handleAs: "json",
                    timeout: 5 * 1000, // Time in milliseconds
                    // The LOAD function will be called on a successful response.
                    load: function (response, ioArgs) { //
                        var permission = response.permission;
                        thisB.permission = permission;
                        var username = response.username;
                        thisB.username = username;
                        if (loadCallback) {
                            loadCallback(permission);
                        }
                    },
                    error: function (response, ioArgs) { //
                        success = false;
                    }
                });
                return success;
            },

            initPopupDialog: function () {
                if (AnnotTrack.popupDialog) {
                    return;
                }
                var track = this;
                var id = "popup_dialog";

                // deregister widget (needed if changing refseq without reloading page)
                var widget = registry.byId(id);
                if (widget) {
                    widget.destroy();
                }
                AnnotTrack.popupDialog = new dojoxDialogSimple({
                    preventCache: true,
                    refreshOnShow: true,
                    executeScripts: true,
                    id: id
                });
                dojo.connect(AnnotTrack.popupDialog, "onHide", AnnotTrack.popupDialog, function () {
                    document.activeElement.blur();
                    track.selectionManager.clearSelection();
                    if (track.getSequenceTrack()) {
                        track.getSequenceTrack().clearHighlightedBases();
                    }

                    dojo.style(dojo.body(), 'overflow', 'auto');
                    document.body.scroll = ''; // needed for ie6/7


                });

                dojo.connect(AnnotTrack.popupDialog, 'onShow', function () {
                    dojo.style(dojo.body(), 'overflow', 'hidden');
                    document.body.scroll = 'no'; // needed for ie6/7
                });


                AnnotTrack.popupDialog.startup();

            },

            getUniqueTrackName: function () {
                return this.refSeq.name;
            },

            openDialog: function (title, data, width, height) {
                AnnotTrack.popupDialog.set("title", title);
                AnnotTrack.popupDialog.set("content", data);
                AnnotTrack.popupDialog.set("style", "width:" + (width ? width : "auto") + ";height:" + (height ? height : "auto"));
                AnnotTrack.popupDialog.show();
            },

            closeDialog: function () {
                AnnotTrack.popupDialog.hide();
            },

            updateMenu: function () {
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
            },

            updateDeleteMenuItem: function () {
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

            updateSetTranslationStartMenuItem: function () {
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
                    menuItem.set("label", "Unset Translation Start");
                }
                else {
                    menuItem.set("label", "Set Translation Start");
                }
            },

            updateSetTranslationEndMenuItem: function () {
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
                    menuItem.set("label", "Unset Translation End");
                }
                else {
                    menuItem.set("label", "Set Translation End");
                }
            },

            updateSetLongestOrfMenuItem: function () {
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

            updateSetReadthroughStopCodonMenuItem: function () {
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
                    menuItem.set("label", "Unset Readthrough Stop Codon");
                }
                else {
                    menuItem.set("label", "Set Readthrough Stop Codon");
                }
            },

            updateMergeMenuItem: function () {
                var menuItem = this.getMenuItem("merge");
                var selected = this.selectionManager.getSelection();
                if (selected.length < 2) {
                    menuItem.set("disabled", true);
                    return;
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

            updateSplitMenuItem: function () {
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

            updateMakeIntronMenuItem: function () {
                var menuItem = this.getMenuItem("make_intron");
                var selected = this.selectionManager.getSelection();
                if (selected.length > 1) {
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

            updateFlipStrandMenuItem: function () {
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

            updateAnnotationInfoEditorMenuItem: function () {
                var menuItem = this.getMenuItem("annotation_info_editor");
                var selected = this.selectionManager.getSelection();
                var parent = AnnotTrack.getTopLevelAnnotation(selected[0].feature);
                for (var i = 1; i < selected.length; ++i) {
                    if (AnnotTrack.getTopLevelAnnotation(selected[i].feature) != parent) {
                        menuItem.set("disabled", true);
                        return;
                    }
                }
                menuItem.set("disabled", false);
            },

            updateUndoMenuItem: function () {
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

            updateRedoMenuItem: function () {
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


            updateHistoryMenuItem: function () {
                var menuItem = this.getMenuItem("history");
                var selected = this.selectionManager.getSelection();
                if (selected.length > 1) {
                    menuItem.set("disabled", true);
                    return;
                }
                menuItem.set("disabled", false);
            },

            updateZoomToBaseLevelMenuItem: function () {
                var menuItem = this.getMenuItem("zoom_to_base_level");
                if (!this.gview.isZoomedToBase()) {
                    menuItem.set("label", "Zoom to Base Level");
                }
                else {
                    menuItem.set("label", "Zoom Back Out");
                }
            },

            updateDuplicateMenuItem: function () {
                var menuItem = this.getMenuItem("duplicate");
                var selected = this.selectionManager.getSelection();
                var parent = AnnotTrack.getTopLevelAnnotation(selected[0].feature);
                for (var i = 1; i < selected.length; ++i) {
                    if (AnnotTrack.getTopLevelAnnotation(selected[i].feature) != parent) {
                        menuItem.set("disabled", true);
                        return;
                    }
                }
                menuItem.set("disabled", false);
            },

            updateSetAsFivePrimeEndMenuItem: function () {
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

            updateSetAsThreePrimeEndMenuItem: function () {
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

            updateSetBothEndsMenuItem: function () {
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

            updateSetNextDonorMenuItem: function () {
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

            updateSetPreviousDonorMenuItem: function () {
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

            updateSetNextAcceptorMenuItem: function () {
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

            updateSetPreviousAcceptorMenuItem: function () {
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

            getMenuItem: function (operation) {
                return annot_context_menu.getChildren()[contextMenuItems[operation]];
            },

            sortAnnotationsByLocation: function (annots) {
                var track = this;
                return annots.sort(function (annot1, annot2) {
                    var start1 = annot1.get("start");
                    var end1 = annot1.get("end");
                    var start2 = annot2.get("start");
                    var end2 = annot2.get('end');

                    if (start1 != start2) {
                        return start1 - start2;
                    }
                    else if (end1 != end2) {
                        return end1 - end2;
                    }
                    else {
                        return annot1.id().localeCompare(annot2.id());
                    }
                });
            },


            /**
             * handles adding overlay of sequence residues to "row" of selected feature
             * (also handled in similar manner in fillBlock()); WARNING: this _requires_
             * browser support for pointer-events CSS property, (currently supported by
             * Firefox 3.6+, Chrome 4.0+, Safari 4.0+) (Exploring possible workarounds
             * for IE, for example see:
             * http://www.vinylfox.com/forwarding-mouse-events-through-layers/
             * http://stackoverflow.com/questions/3680429/click-through-a-div-to-underlying-elements [
             * see section on CSS conditional statement workaround for IE ] ) and must
             * set "pointer-events: none" in CSS rule for div.annot-sequence otherwise,
             * since sequence overlay is rendered on top of selected features (and is a
             * sibling of feature divs), events intended for feature divs will get
             * caught by overlay and not make it to the feature divs
             */
            selectionAdded: function (rec, smanager) {
                var feat = rec.feature;
                this.inherited(arguments);
                var track = this;

                // switched to only have most recent selected annot have residues overlay if
                // zoomed to base level,
                // rather than all selected annots
                // therefore want to revove all prior residues overlay divs
                if (rec.track === track) {
                    // remove sequence text nodes
                    $("div.annot-sequence", track.div).remove();
                }

                // want to get child of block, since want position relative to block
                // so get top-level feature div (assumes top level feature is always
                // rendered...)
                var topfeat = AnnotTrack.getTopLevelAnnotation(feat);
                var featdiv = track.getFeatDiv(topfeat);
                if (featdiv) {
                    if (this.currentResizableFeature && feat.id() == this.currentResizableFeature.id()) {
                        this.makeResizable(this.getFeatDiv(feat));
                    }
                    var strand = topfeat.get('strand');
                    var selectionYPosition = $(featdiv).position().top;
                    var scale = track.gview.bpToPx(1);
                    var charSize = track.webapollo.getSequenceCharacterSize();
                    if (scale === charSize.width && track.useResiduesOverlay) {
                        var seqTrack = this.getSequenceTrack();
                        for (var bindex = this.firstAttached; bindex <= this.lastAttached; bindex++) {
                            var blk = this.blocks[bindex];
                            // seqTrack.getRange(block.startBase, block.endBase,
                            // seqTrack.sequenceStore.getRange(this.refSeq,
                            // block.startBase, block.endBase,
                            // seqTrack.sequenceStore.getFeatures({ ref: this.refSeq.name, start:
                            // block.startBase, end: block.endBase },
                            // function(feat) {
                            seqTrack.sequenceStore.getReferenceSequence(
                                {ref: this.refSeq.name, start: blk.startBase, end: blk.endBase},
                                function (block) {
                                    return function (seq) {
                                        // var start = feat.get('start');
                                        // var end = feat.get('end');
                                        // var seq = feat.get('seq');
                                        var start = block.startBase;
                                        var end = block.endBase;

                                        // var ypos = $(topfeat).position().top;
                                        // +2 hardwired adjustment to center (should be
                                        // calc'd based on feature div dims?
                                        var ypos = selectionYPosition + 2;
                                        // checking to see if residues for this "row" of the
                                        // block are already present
                                        // ( either from another selection in same row, or
                                        // previous rendering
                                        // of same selection [which often happens when
                                        // scrolling] )
                                        // trying to avoid duplication both for efficiency
                                        // and because re-rendering of text can
                                        // be slighly off from previous rendering, leading
                                        // to bold / blurry text when overlaid

                                        var $seqdivs = $("div.annot-sequence", block.domNode);
                                        var sindex = $seqdivs.length;
                                        var add_residues = true;
                                        if ($seqdivs && sindex > 0) {
                                            for (var i = 0; i < sindex; i++) {
                                                var sdiv = $seqdivs[i];
                                                if ($(sdiv).position().top === ypos) {
                                                    // console.log("residues already present
                                                    // in block: " + bindex);
                                                    add_residues = false;
                                                }
                                            }
                                        }
                                        if (add_residues) {
                                            var seqNode = document.createElement("div");
                                            seqNode.className = "annot-sequence";
                                            if (strand == '-' || strand == -1) {
                                                // seq = track.reverseComplement(seq);
                                                seq = track.getSequenceTrack().complement(seq);
                                            }
                                            seqNode.appendChild(document.createTextNode(seq));
                                            // console.log("ypos: " + ypos);
                                            seqNode.style.cssText = "top: " + ypos + "px;";
                                            block.domNode.appendChild(seqNode);
                                            if (track.FADEIN_RESIDUES) {
                                                $(seqNode).hide();
                                                $(seqNode).fadeIn(1500);
                                            }
                                        }
                                    };
                                }(blk));
                        }
                    }
                }
            },

            selectionRemoved: function (selected_record, smanager) {
                // console.log("AnnotTrack.selectionRemoved() called");
                this.inherited(arguments);
                var track = this;
                if (selected_record.track === track) {
                    var feat = selected_record.feature;
                    var featdiv = this.getFeatDiv(feat);
                    // remove sequence text nodes
                    // console.log("removing base residued text from selected annot");
                    $("div.annot-sequence", track.div).remove();
                    delete this.currentResizableFeature;
                    $(featdiv).resizable("destroy");
                }
            },

            startZoom: function (destScale, destStart, destEnd) {

                this.inherited(arguments);

                var selected = this.selectionManager.getSelection();
                if (selected.length > 0) {
                    // if selected annotations, then hide residues overlay
                    // (in case zoomed in to base pair resolution and the residues
                    // overlay is being displayed)
                    $(".annot-sequence", this.div).css('display', 'none');
                }
            },

            executeUpdateOperation: function (postData, loadCallback) {
                console.log('connected and sending notifications');
                this.client.send("/app/AnnotationNotification", {}, JSON.stringify(postData));
                console.log('sent notification message');
            },

            isProteinCoding: function (feature) {
                var topLevelFeature = AnnotTrack.getTopLevelAnnotation(feature);
                if (topLevelFeature.afeature.parent_type && topLevelFeature.afeature.parent_type.name == "gene" && (topLevelFeature.get("type") == "transcript" || topLevelFeature.get("type") == "mRNA")) {
                    return true;
                }
                return false;
            },

            isLoggedIn: function () {
                return this.username != undefined && this.username != 'Guest';
            },

            hasWritePermission: function () {
                return this.permission & Permission.WRITE;
            },

            isAdmin: function () {
                return this.permission & Permission.ADMIN;
            },

            canEdit: function (feature) {
                if (feature) {
                    feature = AnnotTrack.getTopLevelAnnotation(feature);
                }
                return this.hasWritePermission() && (feature ? !feature.get("locked") : true);
            },

            processParent: function (feature, operation) {
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

            }

        });

        AnnotTrack.getTopLevelAnnotation = function (annotation) {
            while (annotation.parent()) {
                annotation = annotation.parent();
            }
            return annotation;
        };

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
