define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'dojo/promise/all',
            'JBrowse/View/Track/HTMLVariants',
            'WebApollo/FeatureSelectionManager',
            'dijit/Menu',
            'dijit/MenuItem',
            'dijit/CheckedMenuItem',
            'dijit/MenuSeparator',
            'dijit/Dialog',
            'jquery',
            'jqueryui/draggable',
            'JBrowse/Util',
            'JBrowse/Model/SimpleFeature',
            'WebApollo/SequenceOntologyUtils'

        ],

        function(
            declare,
            array,
            all,
            HTMLVariantTrack,
            FeatureSelectionManager,
            dijitMenu,
            dijitMenuItem,
            dijitCheckedMenuItem,
            dijitMenuSeparator,
            dijitDialog,
            $,
            draggable,
            Util,
            SimpleFeature,
            SeqOnto ) {

var debugFrame = false;
// this is class
var draggableTrack = declare( HTMLVariantTrack,

{
    dragging: false,

    _defaultConfig: function() {
        return Util.deepUpdate(
            dojo.clone(this.inherited(arguments)),
            {
                style: {
                    // style for this track from webapollo_track_styles.css
                    className: "green-ibeam",
                    renderClassName: "green-ibeam-render"
                },
                events: {
                    click: function(event) {
                        // overriding JBrowse's click behavior
                        event.stopPropagation();
                    }
                }
            }
        );
    },

    constructor: function( args ) {
        this.gview = this.browser.view;
        // get a handle on the main WA object
        this.browser.getPlugin( 'WebApollo', dojo.hitch( this, function(p) {
            this.webapollo = p;
        }));

        this.setSelectionManager(this.webapollo.featSelectionManager);
        // CSS class for selected features
        this.selectionClass = "selected-feature";

        this.last_whitespace_mousedown_loc = null;
        this.last_whitespace_mouseup_time = new Date();
        this.prev_selection = null;

        this.verbose = false;
        this.verbose_selection = false;
        this.verbose_selection_notification = false;
        this.verbose_drag = false;
        this.drag_enabled = true;

        this.feature_context_menu = null;
        this.edge_matching_enabled = true;

    },

    loadSuccess: function(trackInfo) {
        if (! this.has_custom_context_menu) {
            this.initFeatureContextMenu();
            this.initFeatureDialog();
        }
        this.inherited( arguments );
    },

    setSelectionManager: function(selman) {
        if (this.selectionManager) {
            this.selectionManager.removeListener(this);
        }
        this.selectionManager = selman;
        this.selectionManager.addListener(this);
        return selman;

    },

    setViewInfo: function(genomeView, numBlocks, trackDiv, labelDiv, widthPct, widthPx, scale) {
        this.inherited( arguments );
        var $div = $(this.div);
        var track = this;

        // setting up mousedown and mouseup handlers to enable click-in-whitespace to clear selection
        // without conflicting with JBrowse drag-in-whitespace to scroll
        $div.bind('mousedown', function(event) {
            var target = event.target;
            if (! (target.feature || target.subfeature)) {
                // event not on a feature, so must be on whitespace
                track.last_whitespace_mousedown_loc = [ event.pageX, event.pageY ];
            }
        });

        $div.bind('mouseup', function(event) {
            var target = event.target;
            if (! (target.feature || target.subfeature)) {
                // event not on a feature, so must be on whitespace
                var xup = event.pageX;
                var yup = event.pageY;
                // if click in whitespace without dragging and no shift modifier, then deselect all
                if (this.verbose_selection) { console.log("mouse up on track whitespace"); }
                var eventModifier = event.shiftKey || event.altKey || event.metaKey || event.ctrlKey;
                if (track.last_whitespace_mousedown_loc &&
                    xup == track.last_whitespace_mousedown_loc[0] &&
                    yup == track.last_whitespace_mousedown_loc[1] &&
                    (! eventModifier)) {
                    var timestamp = new Date();
                    var prev_timestamp = track.last_whitespace_mouseup_time;
                    track.last_whitespace_mouseup_time = timestamp;
                    // if time is less than half a second, probably a double click
                    var probably_doubleclick = ((timestamp.getTime() - prev_timestamp.getTime()) < 500);
                    if (probably_doubleclick) {
                        if (this.verbose_selection) { console.log("mouse up probably part of a double click"); }
                    }
                    else {
                        track.prev_selection = track.selectionManager.getSelection();
                        if (this.verbose_selection) { console.log("recording prev selection", track.prev_selection); }
                    }
                    if (this.verbose_selection) { console.log("clearing selection"); }
                    track.selectionManager.clearAllSelection();
                }
                else {
                    track.prev_selection = null;
                }
            }
            // mouseup clears tracking of mousedown
            track.last_whitespace_mousedown_loc = null;
        });

        // restore selection, after a double click, to whatever was selected before initiation of a doubleclick
        $div.bind('dblclick', function(event) {
            var target = event.target;

            if (! (target.feature || target.subfeature)) {
                if (this.verbose_selection) {
                    console.log("double click on track whitespace");
                    console.log("restoring selection after double click");
                    console.log(track.prev_selection);
                }
                if (track.prev_selection) {
                    var plength = track.prev_selection.length;
                    // restore selection
                    for (var i = 0; i < plength; i++) {
                        track.selectionManager.addToSelection(track.prev_selection[i]);
                    }
                }
            }
            track.prev_selection = null;
        });

        /// track click diagnostic
        $div.bind("click", function(event) {
            //console.log("track click, base position: ", track.getGenomeCoord(event));
            var target = event.target;
            if (target.feature || target.subfeature) {
                event.stopPropagation();
            }
        });
    },

    selectionAdded: function(rec, smanager) {
        var track = this;
        if (rec.track === track) {
            var featdiv = track.getFeatDiv(rec.feature);
            if (track.verbose_selection_notification) {
                console.log("DraggableHTMLVariants.selectionAdded() called: ", rec, featdiv);
            }
            if (featdiv) {
                var jq_featdiv = $(featdiv);
                if (!jq_featdiv.hasClass(track.selectionClass)) {
                    jq_featdiv.addClass(track.selectionClass);
                }
            }
        }
    },

    selectionCleared: function(selected, smanager) {
        var track = this;
        if (track.verbose_selection_notification) {
            console.log("DraggableHTMLVariants.selectionCleared() called");
        }
        var slength = selected.length;
        for (var i = 0; i < slength; i++) {
            var rec = selected[i];
            track.selectionRemoved(rec);
        }
    },

    selectionRemoved: function(rec, smanager) {
        var track =  this;
        if (rec.track === track) {
            var featdiv = track.getFeatDiv(rec.feature);
            if (track.verbose_selection_notification) {
                console.log("DraggableHTMLVariants.selectionRemoved() called: ", rec, featdiv);
            }
            if (featdiv) {
                var jq_featdiv = $(featdiv);
                if (jq_featdiv.hasClass(track.selectionClass)) {
                    jq_featdiv.removeClass(track.selectionClass);
                }
                if (jq_featdiv.hasClass("ui-draggable")) {
                    jq_featdiv.draggable("destroy");
                }
                if (jq_featdiv.hasClass("ui-multidraggable")) {
                    jq_featdiv.multidraggable("destroy");
                }
            }
        }
    },

    /* overriding renderFeature to add event handling for mouseover, mousedown and mouseup event */
    renderFeature: function(feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd, rclass, clsName) {
        var featdiv = this.inherited(arguments);
        if (featdiv) {
            var $featdiv = $(featdiv);
            $featdiv.bind("mousedown", dojo.hitch(this, 'onFeatureMouseDown'));
            $featdiv.bind("dblclick", dojo.hitch(this, 'onFeatureDoubleClick'));
            if (this.feature_context_menu && (! this.has_custom_context_menu)) {
                this.feature_context_menu.bindDomNode(featdiv);
            }

            if (!rclass) {
                rclass = this.config.style.renderClassName;
            }
            if (rclass) {
                var renderdiv = document.createElement("div");
                dojo.addClass(renderdiv, "feature-render");
                dojo.addClass(renderdiv, rclass);
                if (Util.is_ie6) renderdiv.appendChild(document.createComment());
                featdiv.appendChild(renderdiv);
            }
            if (clsName) {
                dojo.removeClass(featdiv.firstChild, feature.get("type"));
                dojo.addClass(featdiv.firstChild, clsName);
            }
        }
        return featdiv;
    },

    // we wont be needing the following functions for variants
    // renderSubfeature
    // _subfeatSorter
    // _processTranslation
    // handleSubFeatures
    // renderExonSegments

    onFeatureMouseDown: function(event) {
        if (this.verbose_selection || this.verbose_drag) {
            console.log("DraggableHTMLVariants.onFeatureMouseDown called");
            console.log("genome coord: ", this.getGenomeCoord(event));
        }

        this.handleFeatureSelection(event);
        if (this.drag_enabled) {
            this.handleFeatureDragSetup(event);
        }
    },

    handleFeatureSelection: function(event) {
        var ftrack = this;
        var selman = ftrack.selectionManager;
        var featdiv = (event.currentTarget || event.srcElement);
        var feat = featdiv.feature || featdiv.subfeature;

        if (selman.unselectableTypes[feat.get('type')]) {
            return;
        }

        var already_selected = selman.isSelected( {feature: feat, track: ftrack} );
        var parent_selected = false;
        // TODO: would SNV ever have a parent?
        var parent = feat.parent();
        if (parent) {
            console.log("DraggableHTMLVariants.handleFeatureSelection(): feat has a parent: ", parent);
            parent_selected = selman.isSelected( {feature: parent, track: ftrack} );
        }
        if (this.verbose_selection) {
            console.log("DraggableHTMLVariants.handleFeatureSelection() called, actual mouse event");
            console.log(featdiv);
            console.log(feat);
        }

        if (!parent_selected) {
            event.stopPropagation();
        }
        if (Event.shiftKey) {
            if (already_selected) {
                // if event is shift + mouse-down and this feat is already selected, then deselect
                selman.removeFromSelection( {feature: feat, track: this} );
            }
            else if (parent_selected) {
                // if event is shift + mouse-down and parent is selected, do nothing
            }
            else {
                // if event is shift + mouse-down and neither this nor parent is selected, then select this
                selman.addToSelection( {feature: feat, track: this}, true );
            }
        }
        else if (event.altKey) {
        }
        else if (event.ctrlKey) {
        }
        else if (event.metaKey) {
        }
        else {
            // no shift key
            if (already_selected) {
                // if this is already seleected then do nothing
                if (this.verbose_selection) { console.log("already selected"); }
            }
            else {
                if (parent_selected) {
                    // if this is not selected but parent is selected, do nothing
                }
                else {
                    // if this is not selected and parent is not selected, select this
                    selman.clearSelection();
                    selman.addToSelection( {track: this, feature: feat} );
                }
            }
        }
    },

    handleFeatureDragSetup: function(event) {
        var ftrack = this;
        var featdiv = (event.currentTarget || event.srcElement);
        if (this.verbose_drag) { console.log("DraggableHTMLVariants.handleFeatureSelection() called: ", featdiv); }
        var feat = featdiv.feature || featdiv.subfeature;
        var selected = this.selectionManager.isSelected( { feature: feat, track: ftrack } );

        if (selected) {
            var $featdiv = $(featdiv);
            if (! $featdiv.hasClass("ui-draggable")) {
                if (this.verbose_drag) {
                    console.log("Setting up draggability for: ", featdiv);
                }
                var atrack = ftrack.webapollo.getAnnotTrack();
                if (!atrack) {
                    atrack = ftrack.webapollo.getSequenceTrack();
                }
                var fblock = ftrack.getBlock(featdiv);

                // append drag ghost to featdiv block's equivalent block in annotation track, if present
                // else, append to equivalent block in sequence track, if present
                // else append to featdiv's block
                var ablock = (atrack ? atrack.getEquivalentBlock(fblock) : fblock);

                $featdiv.draggable(
                    {
                        zIndex: 200,
                        appendTo: ablock.domNode,

                        helper: function() {
                            var $pfeatdiv;
                            if (featdiv.subfeature) {
                                $pfeatdiv = $(featdiv.parentNode);
                            }
                            else {
                                $pfeatdiv = $(featdiv);
                            }
                            var $holder = $pfeatdiv.clone();
                            $holder.removeClass();
                            // we just want the shell of the top-lvel feature, so remove children
                            $holder.empty();
                            $holder.addClass("custom-multifeature-draggable-helper");
                            var holder = $holder[0];

                            var foffset = $pfeatdiv.offset();
                            var fheight = $pfeatdiv.height();
                            var fwidth = $pfeatdiv.width();
                            var ftop = foffset.top;
                            var fleft = foffset.left;

                            if (this.verbose_drag) {
                                console.log("featdiv dimensions: ", foffset, " height: ", fheight, " width: ", fwidth);
                            }
                            var selection = ftrack.selectionManager.getSelection();
                            var selength = selection.length;

                            for (var i = 0; i < selength; i++) {
                                var srec = selection[i];
                                var strack = srec.track;
                                var sfeat = srec.feature;
                                var sfeatdiv = strack.getFeatDiv(sfeat);
                                if (sfeatdiv) {
                                    var $sfeatdiv = $(sfeatdiv);
                                    var $divclone = $sfeatdiv.clone();
                                    var soffset = $sfeatdiv.offset();
                                    var sheight = $sfeatdiv.height();
                                    var swidth = $sfeatdiv.width();
                                    var seltop = soffset.top;
                                    var sleft = soffset.left;
                                    $divclone.width(swidth);
                                    $divclone.height(sheight);
                                    var delta_top = seltop - ftop;
                                    var delta_left = sleft - fleft;
                                    if (this.verbose_drag) {
                                        console.log(sfeatdiv);
                                        console.log("delta_left: ", delta_left, " delta_top: ", delta_top);
                                    }
                                    //  setting left and top by pixel, based on delta relative to moused-on feature
                                    $divclone.css("left", delta_left);
                                    $divclone.css("top", delta_top);
                                    var divclone = $divclone[0];
                                    holder.appendChild(divclone);
                                }
                            }
                            if (this.verbose_drag) { console.log(holder); }
                            return holder;
                        },
                        opacity: 0.5,
                        axis: 'y'
                });

                $featdiv.draggable().data("draggable")._mouseDown(event);
            }
        }
    },

    /* given a feature or subfeature, return block that rendered it */
    getBlock: function(featdiv) {
        var fdiv = featdiv;
        while (fdiv.feature || fdiv.subfeature) {
            if (fdiv.parentNode.block) {
                return fdiv.parentNode.block;
            }
            fdiv = fdiv.parentNode;
        }
        // should never get here
        return null;
    },

    getEquivalentBlock: function(block) {
        var startBase = block.startBase;
        var endBase = block.endBase;

        for (var i = this.firstAttached; i <= this.lastAttached; i++) {
            var testBlock = this.blocks[i];
            if (testBlock.startBase == startBase && testBlock.endBase == endBase) {
                return testBlock;
            }
        }
        return null;
    },

    onFeatureDoubleClick: function(event) {
        var ftrack = this;
        var selman = ftrack.selectionManager;
        // prevent event bubbling up to genome view and triggering zoom
        event.stopPropagation();
        var featdiv = (event.currentTarget || event.srcElement);
        if (this.verbose_selection) {
            console.log("DraggableHTMLVariants.featDoubleClick: ", ftrack, featdiv);
        }

        var subfeat = featdiv.subfeature;
        if (subfeat && selman.isSelected( {feature: subfeat, track: ftrack} )) {
            // only allow double-click of child for parent selection if child is already selected
            var parent = subfeat.parent();
            if (parent) {
                selman.addToSelection( {feature: parent, track: ftrack} );
            }
        }
    },

    /* Return first feature or subfeature div (including itself) */
    getLowestFeatureDiv: function(elem) {
        while (!elem.feature && !elem.subfeature) {
            elem = elem.parentNode;
            if (elem === document) {
                return null;
            }
        }
        return elem;
    },

    showRange: function(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {
        this.inherited(arguments);
        // redo selection styles for divs in case any divs for selected features were changed/added/deleted
        var srecs = this.selectionManager.getSelection();
        for (var sin in srecs) {
            // only look for selected features in this track
            var srec = srecs[sin];
            if (srec.track === this) {
                // some or all feature divs are usually recreted in a showRange call
                // therefore calling track.selectionAdded() to retrigger setting of selected-feature CSS style, etc. on new feat divs
                this.selectionAdded(srec);
            }
        }
    },

    getGenomeCoord: function(mouseEvent) {
        return Math.floor(this.gview.absXtoBp(mouseEvent.pageX));
    },

    _makeFeatureContextMenu: function(featDiv, menuTemplate) {
        var atrack = this.webapollo.getAnnotTrack();
        var menu = this.inherited(arguments);
        menu.addChild(new dijitMenuSeparator());

        this.contextMenuItems = {};
        var createAnnotationMenuItem = new dijitMenuItem( {
            label: "Create new annotation",
            onClick: dojo.hitch(this, function() {
                var selection = this.selectionManager.getSelection();
                this.selectionManager.clearSelection();
                atrack.createVariant(selection);
            })
        } );
        this.contextMenuItems["create_annotation"] = createAnnotationMenuItem;
        menu.addChild(createAnnotationMenuItem);
        dojo.connect(menu, "onOpen", dojo.hitch(this, function() {
            this.updateContextMenu();
        }));
    },

    _getLayout: function () {
        var thisB = this;
        var browser = this.browser;
        var layout = this.inherited(arguments);
        var clabel = this.name + "-collapsed";
        return declare.safeMixin(layout, {
            addRect: function(id, left, right, height, data) {
                var cm = thisB.collapsedMode || browser.cookie(clabel) == "true";
                if (cm) {
                    var pHeight = Math.ceil(height/this.pitchY);
                    this.pTotalHeight = Math.max(this.pTotalHeight || 0, pHeight);
                }
                var ycoord = (data && data.get('strand')) == -1 ? 20 : 0;
                return cm ? ycoord : this.inherited(arguments);
            }
        });
    },

    _trackMenuOptions: function () {
        var thisB = this;
        var browser = this.browser;

        return all([this.inherited(arguments)]).then(function(options) {
            var o = options.shift();
            var clabel = this.name + "-collapsed";
            o = thisB.webapollo.removeItemWithLabel(o, "Pin to top");
            o = thisB.webapollo.removeItemWithLabel(o, "Delete track");
            o.push({
                type: 'dijit/MenuSeparator'
            });
            o.push({
                label: "Collapsed view",
                title: "Collapsed view",
                type: 'dijit/CheckedMenuItem',
                checked: !!('collapsedMode' in thisB ? thisB.collapsedMode : browser.cookie(clabel) == "true"),
                onClick: function(event) {
                    thisB.collapsedMode = this.get("checked");
                    browser.cookie(clabel, this.get("checked") ? "true" : "false");
                    var temp = thisB.showLabels;
                    if (this.get("checked")) {
                        thisB.showLabels = false;
                    }
                    else if (thisB.previouslyShowLabels) {
                        thisB.showLabels = true;
                    }
                    thisB.previouslyShowLabels = temp;
                    delete thisB.trackMenu;
                    thisB.makeTrackMenu();
                    thisB.redraw();
                }
            });
            return o;
        });
    },

    updateContextMenu: function() {
        var atrack = this.webapollo.getAnnotTrack();
        if (!atrack || !atrack.isLoggedIn() || !atrack.hasWritePermission()) {
            this.contextMenuItems["create_annotation"].set("disabled", true);
        }
        else {
            this.contextMenuItems["create_annotation"].set("disabled", false);
        }
    },

    _exportFormats: function() {
        return [
            {name: 'GFF3', label: 'GFF3', fileExt: 'gff3'},
            //{name: 'FASTA', label: 'FASTA', fileExt: 'fasta'},
            {name: 'BED', label: 'BED', fileExt: 'bed'},
            //{name: 'SequinTable', label: 'Sequin Table', fileExt: 'sqn'}
        ];
    },

    updateFeatureLabelPositions: function(coords) {
        var showLabels = this.webapollo._showLabels;
        if (! 'x' in coords) {
            return;
        }

        array.forEach(this.blocks, function(block, blockIndex) {

            // calculate the view left coord relative to the block left coord
            // in units of pct of the block width
            if (! block || ! this.label) {
                return;
            }

            var viewLeft = 100 * ((this.label.offsetLeft + (showLabels ? this.label.offsetWidth : 0)) - block.domNode.offsetLeft) / block.domNode.offsetWidth + 2;

            // if the view start is unknown, or is to the left of this block, we don't have to worry aobut
            // adjusting the feature labels
            if (!viewLeft) {
                return;
            }
            var blockWidth = block.endBase - block.startBase;
            array.forEach(block.domNode.childNodes, function(featDiv) {
                if (! featDiv.label) {
                    return;
                }
                var labelDiv = featDiv.label;
                var feature = featDiv.feature;

                // get the feature start and end in terms of block width pct
                var minLeft = parseInt(feature.get('start'));
                minLeft = 100 * (minLeft - block.startBase) / blockWidth;
                var maxLeft = parseInt(feature.get('end'));
                maxLeft = 100 * ((maxLeft - block.startBase) / blockWidth - labelDiv.offsetWidth / block.domNode.offsetWidth);

                // move our label div to the view start if the start is between the feature start and end
                labelDiv.style.left = Math.max(minLeft, Math.min(viewLeft, maxLeft)) + "%"
            }, this);
        }, this);
    }

    });
        console.log("Draggable HTML Variants Track object created successfully");
        return draggableTrack;
});