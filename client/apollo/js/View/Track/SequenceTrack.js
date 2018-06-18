define([
        'dojo/_base/declare',
        'dojo/request/xhr',
        'JBrowse/Store/Sequence/StaticChunked',
        'WebApollo/Store/SeqFeature/ScratchPad',
        'WebApollo/View/Track/DraggableHTMLFeatures',
        'WebApollo/JSONUtils',
        'WebApollo/Permission',
        'dojox/widget/Standby',
        'jquery/jquery'
    ],
    function (declare,
              xhr,
              StaticChunked,
              ScratchPad,
              DraggableFeatureTrack,
              JSONUtils,
              Permission,
              Standby,
              $) {

        var SequenceTrack = declare("SequenceTrack", DraggableFeatureTrack,
            {

                /**
                 * Track to display the underlying reference sequence, when zoomed in
                 * far enough.
                 * @class
                 * @constructor
                 */
                constructor: function (args) {
                    this.isWebApolloSequenceTrack = true;
                    var track = this;

                    /**
                     * DraggableFeatureTrack now has its own context menu for divs,
                     * and adding this flag provides a quick way to short-circuit it's
                     * initialization
                     */
                    this.has_custom_context_menu = true;
                    //        this.use_standard_context_menu = false;
                    this.show_reverse_strand = true;
                    this.show_protein_translation = true;
                    this.context_path = "..";
                    this.verbose_server_notification = false;

                    this.residues_context_menu = new dijit.Menu({});  // placeholder till setAnnotTrack() triggers real menu init
                    this.annot_context_menu = new dijit.Menu({});     // placeholder till setAnnotTrack() triggers real menu init

                    this.residuesMouseDown = function (event) {
                        track.onResiduesMouseDown(event);
                    };

//        this.charSize = this.webapollo.getSequenceCharacterSize();
                    //        this.charWidth = this.charSize.charWidth;
                    //        this.seqHeight = this.charSize.seqHeight;

                    // splitting seqHeight into residuesHeight and translationHeight, so future iteration may be possible
                    //    for DNA residues and protein translation to be different styles
                    //        this.dnaHeight = this.seqHeight;
                    //        this.proteinHeight = this.seqHeight;

                    // this.refSeq = refSeq;  already assigned in BlockBased superclass

                    if (this.store.name == 'refseqs') {
                        this.sequenceStore = this.store;
                        var annotStoreConfig = dojo.clone(this.config);
                        annotStoreConfig.browser = this.browser;
                        annotStoreConfig.refSeq = this.refSeq;
                        var annotStore = new ScratchPad(annotStoreConfig);
                        this.store = annotStore;
                        annotStoreConfig.name = this.store.name;
                        this.browser._storeCache[this.store.name] = {
                            refCount: 1,
                            store: this.store
                        };
                    }
                    else {
                        var seqStoreConfig = dojo.clone(this.config);
                        seqStoreConfig.storeClass = "JBrowse/Store/Sequence/StaticChunked";
                        seqStoreConfig.type = "JBrowse/Store/Sequence/StaticChunked";
                        // old style, using residuesUrlTemplate
                        if (this.config.residuesUrlTemplate) {
                            seqStoreConfig.urlTemplate = this.config.residuesUrlTemplate;
                        }

                        var inner_config = dojo.clone(seqStoreConfig);
                        // need a seqStoreConfig.config,
                        //   since in StaticChunked constructor seqStoreConfig.baseUrl is ignored,
                        //   and seqStoreConfig.config.baseUrl is used instead (as of JBrowse 1.9.8+)
                        seqStoreConfig.config = inner_config;
                        // must add browser and refseq _after_ cloning, otherwise get Browser errors
                        seqStoreConfig.browser = this.browser;
                        seqStoreConfig.refSeq = this.refSeq;

                        this.sequenceStore = new StaticChunked(seqStoreConfig);
                        this.browser._storeCache['refseqs'] = {
                            refCount: 1,
                            store: this.sequenceStore
                        };
                    }

                    this.trackPadding = 10;
                    this.SHOW_IF_FEATURES = true;
                    this.ALWAYS_SHOW = false;
                    // this.setLoaded();
                    //        this.initContextMenu();

                    /*
                    var atrack = this.getAnnotTrack();
                    if (atrack)  {
                        this.setAnnotTrack(atrack);
                    }
                    */

                    this.translationTable = {};

                    var initAnnotTrack = dojo.hitch(this, function () {
                        var atrack = this.getAnnotTrack();
                        if (atrack && this.div) {
                            this.setAnnotTrack(atrack);
                        }
                        else {
                            window.setTimeout(initAnnotTrack, 100);
                        }
                    });
                    initAnnotTrack();

                },

// annotSelectionManager is class variable (shared by all AnnotTrack instances)
// SequenceTrack.seqSelectionManager = new FeatureSelectionManager();

// setting up selection exclusiveOr --
//    if selection is made in annot track, any selection in other tracks is deselected, and vice versa,
//    regardless of multi-select mode etc.
// SequenceTrack.seqSelectionManager.addMutualExclusion(DraggableFeatureTrack.selectionManager);
// SequenceTrack.seqSelectionManager.addMutualExclusion(AnnotTrack.annotSelectionManager);
//DraggableFeatureTrack.selectionManager.addMutualExclusion(SequenceTrack.seqSelectionManager);

//    loadSuccess: function(trackInfo)  { }  // loadSuccess no longer called by track initialization/loading
                _defaultConfig: function () {
                    var thisConfig = this.inherited(arguments);
                    // nulling out menuTemplate to suppress default JBrowse feature contextual menu
                    thisConfig.menuTemplate = null;
                    thisConfig.maxFeatureScreenDensity = 100000; // set rediculously high, ensures will never show "zoomed too far out" placeholder
                    thisConfig.style.renderClassName = null;
                    thisConfig.style.arrowheadClass = null;
                    thisConfig.style.centerChildrenVertically = false;
                    thisConfig.ignoreFeatureFilter = true;
                    thisConfig.style.showLabels = false;
                    thisConfig.pinned = true;
                    return thisConfig;
                },

                /** removing "Pin to top" menuitem, so SequenceTrack is always pinned
                 *    and "Delete track" menuitem, so can't be deleted
                 *   (very hacky since depends on label property of menuitem config)
                 */
                _trackMenuOptions: function () {
                    var options = this.inherited(arguments);
                    options = this.webapollo.removeItemWithLabel(options, "Pin to top");
                    options = this.webapollo.removeItemWithLabel(options, "Delete track");
                    return options;
                },

                loadTranslationTable: function () {
                    var track = this;
                    var query = {
                        "track": track.annotTrack.getUniqueTrackName(),
                        "operation": "get_translation_table"
                    };
                    return xhr.post(track.context_path + "/AnnotationEditorService", {
                        data: JSON.stringify(query),
                        handleAs: "json"
                    }).then(function (response) { //
                            track.translationTable = {};
                            var ttable = response.translation_table;
                            track.startProteins = response.start_proteins;
                            track.stopProteins = response.stop_proteins;
                            for (var codon in ttable) {
                                // looping through codon table, make sure not hitting generic properties...
                                if (ttable.hasOwnProperty(codon)) {
                                    var aa = ttable[codon];
                                    var nucs = [];
                                    for (var i = 0; i < 3; i++) {
                                        var nuc = codon.charAt(i);
                                        nucs[i] = [];
                                        nucs[i][0] = nuc.toUpperCase();
                                        nucs[i][1] = nuc.toLowerCase();
                                    }
                                    for (var i = 0; i < 2; i++) {
                                        var n0 = nucs[0][i];
                                        for (var j = 0; j < 2; j++) {
                                            var n1 = nucs[1][j];
                                            for (var k = 0; k < 2; k++) {
                                                var n2 = nucs[2][k];
                                                var triplet = n0 + n1 + n2;
                                                track.translationTable[triplet] = aa;
                                                // console.log("triplet: ", triplet, ", aa: ", aa );
                                            }
                                        }
                                    }
                                }
                            }
                            track.changed();
                        },
                        function (response) {
                            console.log("get_translation_table error", response);
                            return response;
                        });
                },

                /**
                 * called by AnnotTrack to initiate sequence alterations load
                 */
                loadSequenceAlterations: function () {
                    var track = this;

                    var query = {
                        "track": track.annotTrack.getUniqueTrackName(),
                        "operation": "get_sequence_alterations",
                        "organism": track.webapollo.organism,
                        "clientToken": track.getAnnotTrack().getClientToken()
                    };

                    return dojo.xhrPost({
                        postData: JSON.stringify(query),
                        url: track.context_path + "/AnnotationEditorService",
                        handleAs: "json",
                        load: function (response, ioArgs) { //
                            var responseFeatures = response.features;
                            if (!responseFeatures) {
                                alert("Error: " + JSON.stringify(response));
                                return;
                            }
                            for (var i = 0; i < responseFeatures.length; i++) {
                                var jfeat = JSONUtils.createJBrowseSequenceAlteration(responseFeatures[i]);
                                track.store.insert(jfeat);
                            }
                            track.featureCount = track.storedFeatureCount();
                            if (track.ALWAYS_SHOW || (track.SHOW_IF_FEATURES && track.featureCount > 0)) {
                                track.show();
                            }
                            else {
                                track.hide();
                            }
                            track.changed();
                        },
                        error: function (response) {
                            return response;
                        }
                    });
                },

                startZoom: function (destScale, destStart, destEnd) {
                    // would prefer to only try and hide dna residues on zoom if previous scale was at base pair resolution
                    //   (otherwise there are no residues to hide), but by time startZoom is called, pxPerBp is already set to destScale,
                    //    so would require keeping prevScale var around, or passing in prevScale as additional parameter to startZoom()
                    // so for now just always trying to hide residues on a zoom, whether they're present or not

                    $(".dna-residues", this.div).css('display', 'none');
                    $(".block-seq-container", this.div).css('height', '20px');
                    this.heightUpdate(20);
                    this.gview.trackHeightUpdate(this.name, Math.max(this.labelHeight, 20));
                },

                endZoom: function (destScale, destBlockBases) {
                    var charSize = this.webapollo.getSequenceCharacterSize();

                    if ((destScale == charSize.width) ||
                        this.ALWAYS_SHOW || (this.SHOW_IF_FEATURES && this.featureCount > 0)) {
                        this.show();
                    }
                    else {
                        this.hide();
                    }
                    this.clear();
                },

                setViewInfo: function (genomeView, numBlocks,
                                       trackDiv, labelDiv,
                                       widthPct, widthPx, scale) {

                    this.inherited(arguments);

                    var charSize = this.webapollo.getSequenceCharacterSize();
                    if ((scale == charSize.width) ||
                        this.ALWAYS_SHOW || (this.SHOW_IF_FEATURES && this.featureCount > 0)) {
                        this.show();
                    } else {
                        this.hide();
                        this.heightUpdate(0);
                    }
                    this.setLabel(this.key);
                },

                startStandby: function () {
                    if (this.standby == null) {
                        this.standby = new Standby({
                            target: this.div,
                            color: "transparent",
                            image: "plugins/WebApollo/img/loading.gif"
                        });
                        document.body.appendChild(this.standby.domNode);
                        this.standby.startup();
                        this.standby.show();
                    }
                },

                stopStandby: function () {
                    if (this.standby != null) {
                        this.standby.hide();
                    }
                },

                fillBlock: function (args) {
                    var blockIndex = args.blockIndex;
                    var block = args.block;
                    var leftBase = args.leftBase;
                    var rightBase = args.rightBase;
                    var scale = args.scale;
                    var containerStart = args.containerStart;
                    var containerEnd = args.containerEnd;

                    var verbose = false;
                    var fillArgs = arguments;
                    var track = this;

                    var finishCallback = args.finishCallback;
                    args.finishCallback = function () {
                        finishCallback();
                        track.stopStandby();
                    };

                    var charSize = this.webapollo.getSequenceCharacterSize();
                    if ((scale == charSize.width) ||
                        this.ALWAYS_SHOW || (this.SHOW_IF_FEATURES && this.featureCount > 0)) {
                        this.show();
                    } else {
                        this.hide();
                        this.heightUpdate(0);
                    }
                    var blockHeight = 0;

                    if (this.shown) {
                        // make a div to contain the sequences
                        var seqNode = document.createElement("div");
                        seqNode.className = "wa-sequence";
                        // seq_block_container style sets width = 100%, so seqNode fills the block width
                        //    regardless of whether holding residue divs or not
                        $(seqNode).addClass("block-seq-container");
                        block.domNode.appendChild(seqNode);

                        var slength = rightBase - leftBase;

                        // just always add two base pairs to front and end,
                        //    to make sure can do three-frame translation across for every base position in (leftBase..rightBase),
                        //    both forward (need tw pairs on end) and reverse (need 2 extra bases at start)
                        var leftExtended = leftBase - 2;
                        var rightExtended = rightBase + 2;

                        var dnaHeight = charSize.height;
                        var proteinHeight = charSize.height;

                        if (scale == charSize.width) {
                            this.sequenceStore.getReferenceSequence(
                                {ref: this.refSeq.name, start: leftExtended, end: rightExtended},
                                function (seq) {
                                    var start = args.leftBase - 2;
                                    var end = args.rightBase + 2;

                                    if (start < 0) {
                                        start = 0;
                                    }
                                    if (args.leftBase == -1) {
                                        var idx = seq.lastIndexOf(" ");
                                        seq = seq.substring(0, idx) + SequenceTrack.nbsp + seq.substring(idx + 1);
                                    }

                                    var blockStart = start + 2;
                                    var blockEnd = end - 2;
                                    var blockResidues = seq.substring(2, seq.length - 2);
                                    var blockLength = blockResidues.length;
                                    var extendedStart = start;
                                    var extendedEnd = end;
                                    var extendedStartResidues = seq.substring(0, seq.length - 2);
                                    var extendedEndResidues = seq.substring(2);

                                    if (track.show_protein_translation) {
                                        var framedivs = [];
                                        for (var i = 0; i < 3; i++) {
                                            var tstart = blockStart + i;
                                            var frame = tstart % 3;
                                            var transProtein = track.renderTranslation(extendedEndResidues, i, blockLength);
                                            // if coloring CDS in feature tracks by frame, use same "cds-frame" styling,
                                            //    otherwise use more muted "frame" styling
                                            $(transProtein).addClass("cds-frame" + frame);
                                            framedivs[frame] = transProtein;
                                        }
                                        for (var i = 2; i >= 0; i--) {
                                            var transProtein = framedivs[i];
                                            seqNode.appendChild(transProtein);
                                            $(transProtein).bind("mousedown", track.residuesMouseDown);
                                            blockHeight += proteinHeight;
                                        }
                                    }

                                    // add a div for the forward strand
                                    var forwardDNA = track.renderResidues(blockResidues);
                                    $(forwardDNA).addClass("forward-strand");
                                    seqNode.appendChild(forwardDNA);


                                    track.residues_context_menu.bindDomNode(forwardDNA);
                                    $(forwardDNA).bind("mousedown", track.residuesMouseDown);
                                    blockHeight += dnaHeight;

                                    if (track.show_reverse_strand) {
                                        // and one for the reverse strand
                                        // var reverseDNA = track.renderResidues( start, end, track.complement(seq) );
                                        var reverseDNA = track.renderResidues(track.complement(blockResidues));
                                        $(reverseDNA).addClass("reverse-strand");
                                        seqNode.appendChild(reverseDNA);
                                        // dnaContainer.appendChild(reverseDNA);
                                        track.residues_context_menu.bindDomNode(reverseDNA);
                                        $(reverseDNA).bind("mousedown", track.residuesMouseDown);
                                        blockHeight += dnaHeight;
                                    }

                                    // set up highlighting of base pair underneath mouse
                                    $(forwardDNA).bind("mouseleave", function (event) {
                                        track.removeTextHighlight(forwardDNA);
                                        if (reverseDNA) {
                                            track.removeTextHighlight(reverseDNA);
                                        }
                                        track.last_dna_coord = undefined;
                                    });
                                    $(forwardDNA).bind("mousemove", function (event) {
                                        var gcoord = track.getGenomeCoord(event);
                                        if (gcoord >= 0 && ((!track.last_dna_coord) || (gcoord !== track.last_dna_coord))) {
                                            var blockCoord = gcoord - leftBase;
                                            track.last_dna_coord = gcoord;
                                            track.setTextHighlight(forwardDNA, blockCoord, blockCoord, "dna-highlighted");
                                            if (!track.freezeHighlightedBases) {
                                                track.lastHighlightedForwardDNA = forwardDNA;
                                            }
                                            if (reverseDNA) {
                                                track.setTextHighlight(reverseDNA, blockCoord, blockCoord, "dna-highlighted");
                                                if (!track.freezeHighlightedBases) {
                                                    track.lastHighlightedReverseDNA = reverseDNA;
                                                }
                                            }
                                        }
                                        else if (gcoord < 0) {
                                            track.clearHighlightedBases();
                                        }
                                    });
                                    if (reverseDNA) {
                                        $(reverseDNA).bind("mouseleave", function (event) {
                                            track.removeTextHighlight(forwardDNA);
                                            track.removeTextHighlight(reverseDNA);
                                            track.last_dna_coord = undefined;
                                        });
                                        $(reverseDNA).bind("mousemove", function (event) {
                                            var gcoord = track.getGenomeCoord(event);
                                            if (gcoord >= 0 && ((!track.last_dna_coord) || (gcoord !== track.last_dna_coord))) {
                                                var blockCoord = gcoord - leftBase;
                                                track.last_dna_coord = gcoord;
                                                track.setTextHighlight(forwardDNA, blockCoord, blockCoord, "dna-highlighted");
                                                track.setTextHighlight(reverseDNA, blockCoord, blockCoord, "dna-highlighted");
                                                if (!track.freezeHighlightedBases) {
                                                    track.lastHighlightedForwardDNA = forwardDNA;
                                                    track.lastHighlightedReverseDNA = reverseDNA;
                                                }
                                            }
                                            else if (gcoord < 0) {
                                                track.clearHighlightedBases();
                                            }
                                        });
                                    }

                                    if (track.show_protein_translation && track.show_reverse_strand) {
                                        var extendedReverseComp = track.reverseComplement(extendedStartResidues);
                                        if (verbose) {
                                            console.log("extendedReverseComp: " + extendedReverseComp);
                                        }
                                        var framedivs = [];
                                        var offset = (2 - (track.refSeq.length % 3));
                                        for (var i = 2; i >= 0; i--) {
                                            var transStart = blockStart + 1 - i;
                                            var frame = (transStart % 3 + 3) % 3;
                                            frame = (frame + offset) % 3;
                                            var transProtein = track.renderTranslation(extendedStartResidues, i, blockLength, true);
                                            $(transProtein).addClass("neg-cds-frame" + frame);
                                            framedivs[frame] = transProtein;
                                        }
                                        for (var i = 2; i >= 0; i--) {
                                            // for (var i=0; i<3; i++) {
                                            var transProtein = framedivs[i];
                                            seqNode.appendChild(transProtein);
                                            $(transProtein).bind("mousedown", track.residuesMouseDown);
                                            blockHeight += proteinHeight;
                                        }
                                    }
                                    track.inherited("fillBlock", fillArgs);
                                    blockHeight += 5;  // a little extra padding below (track.trackPadding used for top padding)
                                    track.heightUpdate(blockHeight, blockIndex);
                                },
                                function () {
                                }
                            );
                        }
                        else {
                            blockHeight = 20;  // default dna track height if not zoomed to base level
                            seqNode.style.height = "20px";

                            track.inherited("fillBlock", arguments);
                            track.heightUpdate(blockHeight, blockIndex);
                        }
                    } else {
                        this.heightUpdate(0, blockIndex);
                    }
                },

                addFeatureToBlock: function (feature, uniqueId, block, scale, labelScale, descriptionScale,
                                             containerStart, containerEnd) {
                    var featDiv =
                        this.renderFeature(feature, uniqueId, block, scale, labelScale, descriptionScale, containerStart, containerEnd);
                    $(featDiv).addClass("sequence-alteration-artifact");

                    var charSize = this.webapollo.getSequenceCharacterSize();

                    var seqNode = $("div.wa-sequence", block.domNode).get(0);
                    featDiv.style.top = "0px";
                    var ftype = feature.get("type");
                    if (ftype) {
                        if (ftype === "deletion_artifact") {

                        }
                        else if (ftype == "insertion_artifact") {
                            if (scale == charSize.width) {
                                var container = document.createElement("div");
                                var residues = feature.get("residues");
                                $(container).addClass("dna-residues");
                                container.appendChild(document.createTextNode(residues));
                                container.style.position = "absolute";
                                container.style.top = "-16px";
                                container.style.border = "2px solid #00CC00";
                                container.style.backgroundColor = "#AAFFAA";
                                featDiv.appendChild(container);
                            }
                        }
                        else if ((ftype === "substitution_artifact")) {
                            if (scale == charSize.width) {
                                var container = document.createElement("div");
                                var residues = feature.get("residues");
                                $(container).addClass("dna-residues");
                                container.appendChild(document.createTextNode(residues));
                                container.style.position = "absolute";
                                container.style.top = "-16px";
                                container.style.border = "1px solid black";
                                container.style.backgroundColor = "#FFF506";
                                featDiv.appendChild(container);
                            }
                        }
                    }
                    seqNode.appendChild(featDiv);
                    return featDiv;
                },

                /**
                 *  overriding renderFeature to add event handling right-click context menu
                 */
                renderFeature: function (feature, uniqueId, block, scale, labelScale, descriptionScale,
                                         containerStart, containerEnd) {
                    var featDiv = this.inherited(arguments);

                    if (featDiv && featDiv != null) {
                        this.annot_context_menu.bindDomNode(featDiv);
                    }
                    return featDiv;
                },

                reverseComplement: function (seq) {
                    return this.reverse(this.complement(seq));
                },

                reverse: function (seq) {
                    return seq.split("").reverse().join("");
                },

                complement: (function () {
                    var compl_rx = /[ACGT]/gi;

                    // from bioperl: tr/acgtrymkswhbvdnxACGTRYMKSWHBVDNX/tgcayrkmswdvbhnxTGCAYRKMSWDVBHNX/
                    // generated with:
                    // perl -MJSON -E '@l = split "","acgtrymkswhbvdnxACGTRYMKSWHBVDNX"; print to_json({ map { my $in = $_; tr/acgtrymkswhbvdnxACGTRYMKSWHBVDNX/tgcayrkmswdvbhnxTGCAYRKMSWDVBHNX/; $in => $_ } @l})'
                    var compl_tbl = {
                        "S": "S",
                        "w": "w",
                        "T": "A",
                        "r": "y",
                        "a": "t",
                        "N": "N",
                        "K": "M",
                        "x": "x",
                        "d": "h",
                        "Y": "R",
                        "V": "B",
                        "y": "r",
                        "M": "K",
                        "h": "d",
                        "k": "m",
                        "C": "G",
                        "g": "c",
                        "t": "a",
                        "A": "T",
                        "n": "n",
                        "W": "W",
                        "X": "X",
                        "m": "k",
                        "v": "b",
                        "B": "V",
                        "s": "s",
                        "H": "D",
                        "c": "g",
                        "D": "H",
                        "b": "v",
                        "R": "Y",
                        "G": "C"
                    };

                    var compl_func = function (m) {
                        return compl_tbl[m] || SequenceTrack.nbsp;
                    };
                    return function (seq) {
                        return seq.replace(compl_rx, compl_func);
                    };
                })(),

                // given the start and end coordinates, and the sequence bases, creates a div containing the sequence
                renderResidues: function (seq) {
                    var container = document.createElement("div");
                    $(container).addClass("dna-residues");
                    container.appendChild(document.createTextNode(seq));
                    return container;
                },

                // end is ignored, assume all of seq is translated (except any extra bases at end)
                renderTranslation: function (input_seq, offset, blockLength, reverse) {
                    var CodonTable = this.translationTable;
                    var verbose = false;
                    // sequence of diagnostic block
                    //    var verbose = (input_seq === "GTATATTTTGTACGTTAAAAATAAAAA" || input_seq === "GCGTATATTTTGTACGTTAAAAATAAA" );
                    var seq;
                    if (reverse) {
                        seq = this.reverseComplement(input_seq);
                        if (verbose) {
                            console.log("revcomped, input: " + input_seq + ", output: " + seq);
                        }
                    }
                    else {
                        seq = input_seq;
                    }
                    var container = document.createElement("div");
                    $(container).addClass("dna-residues");
                    $(container).addClass("aa-residues");
                    $(container).addClass("offset" + offset);
                    var prefix = "";
                    var suffix = "";
                    for (var i = 0; i < offset; i++) {
                        prefix += SequenceTrack.nbsp;
                    }
                    for (var i = 0; i < (2 - offset); i++) {
                        suffix += SequenceTrack.nbsp;
                    }

                    var extra_bases = (seq.length - offset) % 3;
                    var dnaRes = seq.substring(offset, seq.length - extra_bases);
                    if (verbose) {
                        console.log("to translate: " + dnaRes + ", length = " + dnaRes.length);
                    }
                    var aaResidues = dnaRes.replace(/(...)/gi, function (codon) {
                        var aa = CodonTable[codon];
                        // if no mapping and blank in codon, return blank
                        // if no mapping and no blank in codon,  return "?"
                        if (!aa) {
                            if (codon.indexOf(SequenceTrack.nbsp) >= 0) {
                                aa = SequenceTrack.nbsp;
                            }
                            else {
                                aa = "?";
                            }
                        }
                        return prefix + aa + suffix;
                        // return aa;
                    });
                    var trimmedAaResidues = aaResidues.substring(0, blockLength);
                    if (verbose) {
                        console.log("AaLength: " + aaResidues.length + ", trimmedAaLength = " + trimmedAaResidues.length);
                    }
                    aaResidues = trimmedAaResidues;
                    if (reverse) {
                        var revAaResidues = this.reverse(aaResidues);
                        if (verbose) {
                            console.log("reversing aa string, input: \"" + aaResidues + "\", output: \"" + revAaResidues + "\"");
                        }
                        aaResidues = revAaResidues;
                        while (aaResidues.length < blockLength) {
                            aaResidues = SequenceTrack.nbsp + aaResidues;
                        }
                    }

                    var track = this;

                    var startProtein = track.startProteins;
                    var stopProtein = track.stopProteins;


                    if (startProtein && stopProtein && aaResidues) {

                        var residueString = '';
                        for (var residueIndex in aaResidues) {
                            var residue = aaResidues[residueIndex];
                            if (startProtein.indexOf(residue) >= 0 || stopProtein.indexOf(residue) >= 0) {
                                container.appendChild(document.createTextNode(residueString));
                                residueString = '';
                                if (startProtein.indexOf(residue) >= 0) {
                                    var startDiv = dojo.create('div', {className: 'sequence-start-protein'});
                                    startDiv.appendChild(document.createTextNode(residue));
                                    container.appendChild(startDiv);
                                }
                                else if (stopProtein.indexOf(residue) >= 0) {
                                    var stopDiv = dojo.create('div', {className: 'sequence-stop-protein'});
                                    stopDiv.appendChild(document.createTextNode(residue));
                                    container.appendChild(stopDiv);
                                }
                            }
                            else {
                                residueString += residue;
                            }
                        }
                        container.appendChild(document.createTextNode(residueString));
                    }
                    else {
                        container.appendChild(document.createTextNode(aaResidues));
                    }
                    return container;
                },

                onResiduesMouseDown: function (event) {
                    this.last_mousedown_event = event;
                },

                onFeatureMouseDown: function (event) {
                    this.last_mousedown_event = event;
                    var ftrack = this;
                    if (ftrack.verbose_selection || ftrack.verbose_drag) {
                        console.log("SequenceTrack.onFeatureMouseDown called");
                    }
                    this.handleFeatureSelection(event);
                },

                initContextMenu: function () {
                    var thisObj = this;
                    thisObj.contextMenuItems = new Array();
                    thisObj.annot_context_menu = new dijit.Menu({});

                    var index = 0;
                    if (this.annotTrack.permission & Permission.WRITE) {
                        thisObj.annot_context_menu.addChild(new dijit.MenuItem({
                            label: "Delete",
                            onClick: function () {
                                thisObj.deleteSelectedFeatures();
                            }
                        }));
                        thisObj.contextMenuItems["delete"] = index++;
                    }
                    thisObj.annot_context_menu.addChild(new dijit.MenuItem({
                        label: "Information",
                        onClick: function (event) {
                            thisObj.getInformation();
                        }
                    }));
                    thisObj.contextMenuItems["information"] = index++;

                    thisObj.annot_context_menu.onOpen = function (event) {
                        // keeping track of mousedown event that triggered annot_context_menu popup,
                        //   because need mouse position of that event for some actions
                        thisObj.annot_context_mousedown = thisObj.last_mousedown_event;
                        // if (thisObj.permission & Permission.WRITE) { thisObj.updateMenu(); }
                        dojo.forEach(this.getChildren(), function (item, idx, arr) {
                            if (item._setSelected) {
                                item._setSelected(false);
                            }  // test for function existence first
                            if (item._onUnhover) {
                                item._onUnhover();
                            }  // test for function existence first
                        });
                    };

                    /**
                     *   context menu for right click on sequence residues
                     */
                    thisObj.residuesMenuItems = new Array();
                    thisObj.residues_context_menu = new dijit.Menu({});
                    index = 0;

                    thisObj.residuesMenuItems["toggle_reverse_strand"] = index++;
                    thisObj.residues_context_menu.addChild(new dijit.MenuItem({
                        label: "Toggle Reverse Strand",
                        onClick: function (event) {
                            thisObj.show_reverse_strand = !thisObj.show_reverse_strand;
                            thisObj.clearHighlightedBases();
                            thisObj.changed();
                        }
                    }));

                    thisObj.residuesMenuItems["toggle_protein_translation"] = index++;
                    thisObj.residues_context_menu.addChild(new dijit.MenuItem({
                        label: "Toggle Protein Translation",
                        onClick: function (event) {
                            thisObj.show_protein_translation = !thisObj.show_protein_translation;
                            thisObj.clearHighlightedBases();
                            thisObj.changed();
                        }
                    }));


                    if (this.annotTrack.permission & Permission.WRITE) {

                        thisObj.residues_context_menu.addChild(new dijit.MenuSeparator());
                        thisObj.residues_context_menu.addChild(new dijit.MenuItem({
                            label: "Create Genomic Insertion",
                            onClick: function () {
                                thisObj.freezeHighlightedBases = true;
                                thisObj.createGenomicInsertion();
                            }
                        }));
                        thisObj.residuesMenuItems["create_insertion"] = index++;
                        thisObj.residues_context_menu.addChild(new dijit.MenuItem({
                            label: "Create Genomic Deletion",
                            onClick: function (event) {
                                thisObj.freezeHighlightedBases = true;
                                thisObj.createGenomicDeletion();
                            }
                        }));
                        thisObj.residuesMenuItems["create_deletion"] = index++;

                        thisObj.residues_context_menu.addChild(new dijit.MenuItem({
                            label: "Create Genomic Substitution",
                            onClick: function (event) {
                                thisObj.freezeHighlightedBases = true;
                                thisObj.createGenomicSubstitution();
                            }
                        }));
                        thisObj.residuesMenuItems["create_substitution"] = index++;
                    }

                    thisObj.residues_context_menu.onOpen = function (event) {
                        thisObj.residues_context_mousedown = thisObj.last_mousedown_event;
                        dojo.forEach(this.getChildren(), function (item, idx, arr) {
                            if (item._setSelected) {
                                item._setSelected(false);
                            }
                            if (item._onUnhover) {
                                item._onUnhover();
                            }
                        });

                        thisObj.freezeHighlightedBases = true;
                    };

                    thisObj.residues_context_menu.onBlur = function () {
                        thisObj.freezeHighlightedBases = false;
                    };

                    thisObj.residues_context_menu.onClose = function (event) {
                        if (!thisObj.freezeHighlightedBases) {
                            thisObj.clearHighlightedBases();
                        }
                    };

                    thisObj.annot_context_menu.startup();
                    thisObj.residues_context_menu.startup();
                },

                getUniqueTrackName: function () {
                    return this.name + "-" + this.refSeq.name;
                },

                createGenomicInsertion: function () {
                    var gcoord = this.getGenomeCoord(this.residues_context_mousedown);

                    var content = this.createAddSequenceAlterationPanel("insertion_artifact", gcoord);
                    this.annotTrack.openDialog("Add Insertion", content);
                },

                createGenomicDeletion: function () {
                    var gcoord = this.getGenomeCoord(this.residues_context_mousedown);

                    var content = this.createAddSequenceAlterationPanel("deletion_artifact", gcoord);
                    this.annotTrack.openDialog("Add Deletion", content);

                },

                createGenomicSubstitution: function () {
                    var gcoord = this.getGenomeCoord(this.residues_context_mousedown);
                    var content = this.createAddSequenceAlterationPanel("substitution_artifact", gcoord);
                    this.annotTrack.openDialog("Add Substitution", content);
                },

                deleteSelectedFeatures: function () {
                    var selected = this.selectionManager.getSelection();
                    this.selectionManager.clearSelection();
                    this.requestDeletion(selected);
                },

                requestDeletion: function (selected) {
                    var track = this;
                    var features = "[ ";
                    for (var i = 0; i < selected.length; ++i) {
                        var annot = selected[i].feature;
                        if (i > 0) {
                            features += ", ";
                        }
                        features += '{ "uniquename": "' + annot.id() + '" }';
                    }
                    features += "]";
                    var postData = '{ "track": "' + track.annotTrack.getUniqueTrackName() + '", "features": ' + features + ', "operation": "delete_sequence_alteration" }';
                    track.annotTrack.executeUpdateOperation(postData);
                },

                getInformation: function () {
                    var selected = this.selectionManager.getSelection();
                    var annotTrack = this.getAnnotTrack();
                    if (annotTrack) {
                        annotTrack.getInformationForSelectedAnnotations(selected);
                    }
                },

                /**
                 * sequence alteration annotation ADD command received by a ChangeNotificationListener,
                 *      so telling SequenceTrack to add to it's SeqFeatureStore
                 */
                annotationsAddedNotification: function (responseFeatures) {
                    if (this.verbose_server_notification) {
                        console.log("SequenceTrack.annotationsAddedNotification() called");
                    }
                    var track = this;
                    // add to store
                    for (var i = 0; i < responseFeatures.length; ++i) {
                        var feat = JSONUtils.createJBrowseSequenceAlteration(responseFeatures[i]);
                        var id = responseFeatures[i].uniquename;
                        if (!this.store.getFeatureById(id)) {
                            this.store.insert(feat);
                        }
                    }
                    track.featureCount = track.storedFeatureCount();
                    if (this.ALWAYS_SHOW || (this.SHOW_IF_FEATURES && this.featureCount > 0)) {
                        this.show();
                    }
                    else {
                        this.hide();
                    }
                    // track.hideAll();   shouldn't need to call hideAll() before changed() anymore
                    track.changed();
                },

                /**
                 * sequence alteration annotation DELETE command received by a ChangeNotificationListener,
                 *      so telling SequenceTrack to remove from it's SeqFeatureStore
                 */
                annotationsDeletedNotification: function (annots) {
                    if (this.verbose_server_notification) {
                        console.log("SequenceTrack.removeSequenceAlterations() called");
                    }
                    var track = this;
                    // remove from SeqFeatureStore
                    for (var i = 0; i < annots.length; ++i) {
                        var id_to_delete = annots[i].uniquename;
                        this.store.deleteFeatureById(id_to_delete);
                    }
                    track.featureCount = track.storedFeatureCount();
                    if (this.ALWAYS_SHOW || (this.SHOW_IF_FEATURES && this.featureCount > 0)) {
                        this.show();
                    }
                    else {
                        this.hide();
                    }
                    // track.hideAll();   shouldn't need to call hideAll() before changed() anymore
                    track.changed();
                },

                /*
                 *  sequence alteration UPDATE command received by a ChangeNotificationListener
                 *  currently handled as if receiving DELETE followed by ADD command
                 */
                annotationsUpdatedNotification: function (annots) {
                    this.annotationsDeletedNotification(annots);
                    this.annotationsAddedNotification(annots);
                },

                storedFeatureCount: function (start, end) {
                    // get accurate count of features loaded (should do this within the XHR.load() function
                    var track = this;
                    if (start === undefined) {
                        start = track.refSeq.start;
                    }
                    if (end === undefined) {
                        end = track.refSeq.end;
                    }
                    var count = 0;
                    track.store.getFeatures({ref: track.refSeq.name, start: start, end: end}, function () {
                        count++;
                    });

                    return count;
                },

                createAddSequenceAlterationPanel: function (type, gcoord) {
                    var track = this;
                    var content = dojo.create("div");
                    var charWidth = 15;
                    if (type === "deletion_artifact") {
                        var deleteDiv = dojo.create("div", {}, content);
                        var deleteLabel = dojo.create("label", {
                            innerHTML: "Length",
                            className: "sequence_alteration_input_label"
                        }, deleteDiv);
                        var deleteField = dojo.create("input", {
                            type: "text",
                            size: 10,
                            className: "sequence_alteration_input_field"
                        }, deleteDiv);
                        var comment = dojo.create("div", {}, content);
                        var comLabel = dojo.create("label", {
                            innerHTML: "Comment",
                            className: "sequence_alteration_comment_label"
                        }, comment);
                        var comField = dojo.create("input", {
                            type: "text",
                            size: charWidth,
                            className: "sequence_alteration_comment_field"
                        }, comment);
                        $(deleteField).keydown(function (e) {
                            var unicode = e.charCode || e.keyCode;
                            var isBackspace = (unicode === 8);  // 8 = BACKSPACE
                            if (unicode === 13) {  // 13 = ENTER/RETURN
                                addSequenceAlteration();
                            }
                            else {
                                var newchar = String.fromCharCode(unicode);
                                // only allow numeric chars and backspace
                                if (!(newchar.match(/[0-9]/) || isBackspace)) {
                                    return false;
                                }
                            }
                        });
                    }
                    else {
                        var plusDiv = dojo.create("div", {}, content);
                        var minusDiv = dojo.create("div", {}, content);
                        var plusLabel = dojo.create("label", {
                            innerHTML: "+ strand",
                            className: "sequence_alteration_input_label"
                        }, plusDiv);
                        var plusField = dojo.create("input", {
                            type: "text",
                            size: charWidth,
                            className: "sequence_alteration_input_field"
                        }, plusDiv);
                        var minusLabel = dojo.create("label", {
                            innerHTML: "- strand",
                            className: "sequence_alteration_input_label"
                        }, minusDiv);
                        var minusField = dojo.create("input", {
                            type: "text",
                            size: charWidth,
                            className: "sequence_alteration_input_field"
                        }, minusDiv);
                        var comment = dojo.create("div", {}, content);
                        var comLabel = dojo.create("label", {
                            innerHTML: " Comment",
                            className: "sequence_alteration_comment_label"
                        }, comment);
                        var comField = dojo.create("input", {
                            type: "text",
                            size: charWidth,
                            className: "sequence_alteration_comment_field"
                        }, comment);

                        $(plusField).keydown(function (e) {
                            var unicode = e.charCode || e.keyCode;
                            // ignoring delete key, doesn't do anything in input elements?
                            var isBackspace = (unicode === 8);  // 8 = BACKSPACE
                            if (unicode === 13) {  // 13 = ENTER/RETURN
                                addSequenceAlteration();
                            }
                            else {
                                var curval = e.srcElement.value;
                                var newchar = String.fromCharCode(unicode);
                                // only allow acgtnACGTN and backspace
                                //    (and acgtn are transformed to uppercase in CSS)
                                if (newchar.match(/[acgtnACGTN]/) || isBackspace) {
                                    // can't synchronize scroll position of two input elements,
                                    // see http://stackoverflow.com/questions/10197194/keep-text-input-scrolling-synchronized
                                    // but, if scrolling triggered (or potentially triggered), can hide other strand input element
                                    // scrolling only triggered when length of input text exceeds character size of input element
                                    if (isBackspace) {
                                        minusField.value = track.complement(curval.substring(0, curval.length - 1));
                                    }
                                    else {
                                        minusField.value = track.complement(curval + newchar);
                                    }
                                    if (curval.length > charWidth) {
                                        $(minusDiv).hide();
                                    }
                                    else {
                                        $(minusDiv).show();  // make sure is showing to bring back from a hide
                                    }
                                }
                                else {
                                    return false;
                                }  // prevents entering any chars other than ACGTN and backspace
                            }
                        });

                        $(minusField).keydown(function (e) {
                            var unicode = e.charCode || e.keyCode;
                            // ignoring delete key, doesn't do anything in input elements?
                            var isBackspace = (unicode === 8);  // 8 = BACKSPACE
                            if (unicode === 13) {  // 13 = ENTER
                                addSequenceAlteration();
                            }
                            else {
                                var curval = e.srcElement.value;
                                var newchar = String.fromCharCode(unicode);
                                // only allow acgtnACGTN and backspace
                                //    (and acgtn are transformed to uppercase in CSS)
                                if (newchar.match(/[acgtnACGTN]/) || isBackspace) {
                                    // can't synchronize scroll position of two input elements,
                                    // see http://stackoverflow.com/questions/10197194/keep-text-input-scrolling-synchronized
                                    // but, if scrolling triggered (or potentially triggered), can hide other strand input element
                                    // scrolling only triggered when length of input text exceeds character size of input element
                                    if (isBackspace) {
                                        plusField.value = track.complement(curval.substring(0, curval.length - 1));
                                    }
                                    else {
                                        plusField.value = track.complement(curval + newchar);
                                    }
                                    if (curval.length > charWidth) {
                                        $(plusDiv).hide();
                                    }
                                    else {
                                        $(plusDiv).show();  // make sure is showing to bring back from a hide
                                    }
                                }
                                else {
                                    return false;
                                }  // prevents entering any chars other than ACGTN and backspace
                            }
                        });

                    }
                    var buttonDiv = dojo.create("div", {className: "sequence_alteration_button_div"}, content);
                    var addButton = dojo.create("button", {
                        innerHTML: "Add",
                        className: "sequence_alteration_button"
                    }, buttonDiv);

                    var addSequenceAlteration = function () {
                        var ok = true;
                        var inputField;
                        var commentField = comField;
                        var inputField = ((type === "deletion_artifact") ? deleteField : plusField);
                        // if (type == "deletion") { inputField = deleteField; }
                        // else  { inputField = plusField; }
                        var input = inputField.value.toUpperCase();
                        var commentFieldValue = commentField.value;
                        if (input.length == 0) {
                            alert("Input cannot be empty for " + type);
                            ok = false;
                        }
                        if (ok) {
                            var input = inputField.value.toUpperCase();
                            if (type === "deletion_artifact") {
                                if (input.match(/\D/)) {
                                    alert("The length must be a number");
                                    ok = false;
                                }
                                else {
                                    input = parseInt(input);
                                    if (input <= 0) {
                                        alert("The length must be a positive number");
                                        ok = false;
                                    }
                                }
                            }
                            else {
                                if (input.match(/[^ACGTN]/)) {
                                    alert("The sequence should only containing A, C, G, T, N");
                                    ok = false;
                                }
                            }
                        }
                        if (ok) {
                            var fmin = gcoord;
                            var fmax;
                            if (type === "insertion_artifact") {
                                fmax = gcoord;
                            }
                            else if (type === "deletion_artifact") {
                                fmax = gcoord + parseInt(input);
                            }
                            else if (type === "substitution_artifact") {
                                fmax = gcoord + input.length;
                            }
                            var feature = {
                                location: {
                                    fmin: fmin,
                                    fmax: fmax,
                                    strand: 1
                                },
                                type: {
                                    name: type,
                                    cv: {
                                        name: "sequence"
                                    }
                                }
                            };
                            if (type != "deletion_artifact") {
                                feature.residues = input;
                            }
                            if (commentFieldValue.length != 0) {
                                feature.non_reserved_properties = [
                                    {
                                        tag: "justification",
                                        value: commentFieldValue
                                    }
                                ];
                            }
                            var features = [feature];
                            var postData = {
                                "track": track.annotTrack.getUniqueTrackName(),
                                "features": features,
                                "operation": "add_sequence_alteration",
                                "clientToken": track.annotTrack.getClientToken()
                            };
                            track.annotTrack.executeUpdateOperation(JSON.stringify(postData));
                            track.annotTrack.closeDialog();
                        }
                    };

                    dojo.connect(addButton, "onclick", null, function () {
                        addSequenceAlteration();
                    });

                    return content;
                },

                handleError: function (response) {
                    console.log("ERROR: ");
                    console.log(response);  // in Firebug, allows retrieval of stack trace, jump to code, etc.
                    console.log(response.stack);
                    var error = eval('(' + response.responseText + ')');
                    if (error && error.error) {
                        alert(error.error);
                        return;
                    }
                },

                setAnnotTrack: function (annotTrack) {
                    this.startStandby();
                    // if (this.annotTrack)  { console.log("WARNING: SequenceTrack.setAnnotTrack called but annoTrack already set"); }
                    var track = this;

                    this.annotTrack = annotTrack;
                    this.initContextMenu();

                    this.loadTranslationTable().then(
                        function () {
                            if (track.webapollo.getAnnotTrack().isLoggedIn()) {
                                track.loadSequenceAlterations().then(function () {
                                    track.stopStandby();
                                });
                            }
                        },
                        function () {
                            track.stopStandby();
                        });
                },

                /*
                 * Given an element that contains text, highlights a given range of the text
                 * If called repeatedly, removes highlighting from previous call first
                 *
                 * Assumes there is no additional markup within element, just a text node
                 *    (would like to eventually rewrite to remove this restriction?  Possibly could use HTML Range manipulation,
                 *        i.e. range.surroundContents() etc. )
                 *
                 * optionally specify what class to use to indicate highlighting (defaults to "text-highlight")
                 *
                 * adapted from http://stackoverflow.com/questions/9051369/highlight-substring-in-element
                 */
                setTextHighlight: function (element, start, end, classname) {
                    if (this.freezeHighlightedBases) {
                        return;
                    }
                    if (!classname) {
                        classname = "text-highlight";
                    }
                    var item = $(element);
                    var str = item.text();
                    if (!str) {
                        str = item.html();
                        item.data("origHTML", str);
                    }
                    var highlighted_base = str.substr(start, end - start + 1);
                    if (highlighted_base == SequenceTrack.nbsp) {
                        return;
                    }
                    str = str.substr(0, start) +
                        '<span class="' + classname + '">' +
                        str.substr(start, end - start + 1) +
                        '</span>' +
                        str.substr(end + 1);
                    item.html(str);
                },

                /*
                 *  remove highlighting added with setTextHighlight
                 */
                removeTextHighlight: function (element) {
                    if (this.freezeHighlightedBases) {
                        return;
                    }
                    var item = $(element);
                    var str = item.text();
                    if (str) {
                        item.html(str);
                    }
                },

                clearHighlightedBases: function () {
                    this.freezeHighlightedBases = false;
                    this.removeTextHighlight(this.lastHighlightedForwardDNA);
                    if (this.lastHighlightedReverseDNA) {
                        this.removeTextHighlight(this.lastHighlightedReverseDNA);
                    }
                },

                getAnnotTrack: function () {
                    if (this.annotTrack) {
                        return this.annotTrack;
                    }
                    else {
                        var tracks = this.gview.tracks;
                        for (var i = 0; i < tracks.length; i++) {
                            // should be doing instanceof here, but class setup is not being cooperative
                            if (tracks[i].isWebApolloAnnotTrack) {
                                this.annotTrack = tracks[i];
                                this.annotTrack.seqTrack = this;
                                break;
                            }
                        }
                    }
                    return this.annotTrack;
                },

                hide: function () {
                    this.inherited(arguments);
                    var annotTrack = this.getAnnotTrack();
                    if (annotTrack && !annotTrack.isLoggedIn()) {
                        dojo.style(this.genomeView.pinUnderlay, "display", "none");
                    }
                }

            });

        SequenceTrack.nbsp = String.fromCharCode(160);
        return SequenceTrack;
    });

