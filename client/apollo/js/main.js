require({
           packages: [
               { name: 'jqueryui', location: '../plugins/WebApollo/jslib/jqueryui' },
               { name: 'jquery', location: '../plugins/WebApollo/jslib/jquery', main: 'jquery' },
               { name: 'bbop', location: '../plugins/WebApollo/jslib/bbop' }
           ]
       },
       [],
       function() {

define.amd.jQuery = true;

define(
       [
           'dojo/_base/declare',
           'dijit/MenuItem', 
           'dijit/MenuSeparator', 
           'dijit/CheckedMenuItem',
           'dijit/form/DropDownButton',
           'dijit/DropDownMenu',
           'dijit/form/Button',
           'JBrowse/Plugin',
           './FeatureEdgeMatchManager',
	   './FeatureSelectionManager',
           './TrackConfigTransformer', 
	   './View/Track/AnnotTrack', 
           'JBrowse/View/FileDialog/TrackList/GFF3Driver'
       ],
    function( declare, dijitMenuItem, dijitMenuSeparator, dijitCheckedMenuItem, dijitDropDownButton, dijitDropDownMenu, dijitButton, JBPlugin, 
              FeatureEdgeMatchManager, FeatureSelectionManager, TrackConfigTransformer, AnnotTrack, GFF3Driver ) {

return declare( JBPlugin,
{

    constructor: function( args ) {
        console.log("loaded WebApollo plugin");
        var thisB = this;
        this.colorCdsByFrame = false;
        this.searchMenuInitialized = false;
        var browser = this.browser;  // this.browser set in Plugin superclass constructor
        
        if (browser.config.favicon) {
            // this.setFavicon("plugins/WebApollo/img/webapollo_favicon.ico");
            this.setFavicon(browser.config.favicon);
        }

        args.cssLoaded.then( function() {
            if (! browser.config.view) { browser.config.view = {}; }
            browser.config.view.maxPxPerBp = thisB.getSequenceCharacterSize().width;
        } );

        if (! browser.config.helpUrl)  {
	    browser.config.helpUrl = "http://genomearchitect.org/webapollo/docs/help.html";
        }


        // hand the browser object to the feature edge match manager
        FeatureEdgeMatchManager.setBrowser( browser );

	this.featSelectionManager = new FeatureSelectionManager();
	this.annotSelectionManager = new FeatureSelectionManager();
        this.trackTransformer = new TrackConfigTransformer();

	// setting up selection exclusiveOr --
	//    if selection is made in annot track, any selection in other tracks is deselected, and vice versa,
	//    regardless of multi-select mode etc.
	this.annotSelectionManager.addMutualExclusion(this.featSelectionManager);
	this.featSelectionManager.addMutualExclusion(this.annotSelectionManager);

	FeatureEdgeMatchManager.addSelectionManager(this.featSelectionManager);
	FeatureEdgeMatchManager.addSelectionManager(this.annotSelectionManager);


        // add a global menu option for setting CDS color
        var cds_frame_toggle = new dijitCheckedMenuItem(
                {
                    label: "Color by CDS frame",
                    checked: false,
                    onClick: function(event) {
                        thisB.colorCdsByFrame = cds_frame_toggle.checked;
                        browser.view.redrawTracks();
                    }
                });
        browser.addGlobalMenuItem( 'view', cds_frame_toggle );

        this.addStrandFilterOptions();


        if (browser.config.show_nav) {
            var helpUrl = browser.config.helpUrl;
//            var guideUrl = "http://genomearchitect.org/webapollo/docs/webapollo_user_guide.pdf";
//            var wikiUrl = "http://www.gmod.org/wiki/WebApollo";
            var jbrowseUrl = "http://jbrowse.org";
            browser.addGlobalMenuItem( 'help',
                                    new dijitMenuItem(
                                        {
                                            id: 'menubar_apollo_quickstart', 
                                            label: 'General',
                                            onClick: function()  { window.open(helpUrl,'help_window').focus(); }
                                        })
                                  );
  /*          browser.addGlobalMenuItem( 'help',
                                    new dijitMenuItem(
                                        {
                                            id: 'menubar_apollo_userguide', 
                                            label: 'User Guide',
                                            onClick: function()  { window.open(guideUrl,'help_window').focus(); }
                                        })
                                  );
            browser.addGlobalMenuItem( 'help',
                                    new dijitMenuItem(
                                        {
                                            id: 'menubar_apollo_wiki', 
                                            label: 'Wiki',
                                            onClick: function()  { window.open(wikiUrl,'help_window').focus(); }
                                        })
                                  );
*/
            browser.addGlobalMenuItem( 'help',
                                    new dijitMenuItem(
                                        {
                                            id: 'menubar_powered_by_jbrowse', 
                                            label: 'Powered by JBrowse',
                                            // iconClass: 'jbrowseIconHelp', 
                                            onClick: function()  { window.open(jbrowseUrl,'help_window').focus(); }
                                        })
                                  );
        }

        // register the WebApollo track types with the browser, so
        // that the open-file dialog and other things will have them
        // as options
        browser.registerTrackType({
            type:                 'WebApollo/View/Track/DraggableHTMLFeatures',
            defaultForStoreTypes: [ 'JBrowse/Store/SeqFeature/NCList',
                                    'JBrowse/Store/SeqFeature/GFF3', 
                                    'WebApollo/Store/SeqFeature/ApolloGFF3'
                                  ],
            label: 'WebApollo Features'
        });
        browser.registerTrackType({
            type:                 'WebApollo/View/Track/DraggableAlignments',
            defaultForStoreTypes: [ 
                                    'JBrowse/Store/SeqFeature/BAM',
                                  ],
            label: 'WebApollo Alignments'
        });
        browser.registerTrackType({
            type:                 'WebApollo/View/Track/SequenceTrack',
            defaultForStoreTypes: [ 'JBrowse/Store/Sequence/StaticChunked' ],
            label: 'WebApollo Sequence'
        });

        // transform track configs from vanilla JBrowse to WebApollo:
        // type: "JBrowse/View/Track/HTMLFeatures" ==> "WebApollo/View/Track/DraggableHTMLFeatures"
        //
        var track_configs = browser.config.tracks;
        for (var i=0; i<track_configs.length; i++)  {
            var track_config = track_configs[i];
            this.trackTransformer.transform(track_config);
        }

        // put the WebApollo logo in the powered_by place in the main JBrowse bar
        browser.afterMilestone( 'initView', function() {
        //    dojo.connect( browser.browserWidget, "resize", thisB, 'onResize' );
            if (browser.poweredByLink)  {
                dojo.disconnect(browser.poweredBy_clickHandle);
                browser.poweredByLink.innerHTML = '<img src=\"plugins/WebApollo/img/ApolloLogo_100x36.png\" height=\"25\" />';
                browser.poweredByLink.href = 'http://www.gmod.org/wiki/WebApollo';
                browser.poweredByLink.target = "_blank";
            } 
            
           var view = browser.view;
           view.oldOnResize = view.onResize;
  
         /* trying to fix residues rendering bug when web browser scaling/zoom (Cmd+, Cmd-) is used 
          *    bug appears in Chrome, not Firefox, unsure of other browsers
          */
         view.onResize = function() {  
            // detect if zoomed into base level
            // var fullZoom = (view.pxPerBp == view.maxPxPerBp);
            // if showing residues (full zoom), then pxPerBp == maxPxPerBp
            //     probably shouldn't ever have pxPerBp > maxPxPerBp, but catching and considereing as fullZoom as well, just in case
            var fullZoom = (view.pxPerBp >= view.maxPxPerBp);
            var centerBp = Math.round((view.minVisible() + view.maxVisible())/2); 
            var oldCharSize = thisB.getSequenceCharacterSize();
            var newCharSize = thisB.getSequenceCharacterSize(true);
            // detect if something happened to change pixel size of residues font (likely a web browser zoom)
            var charWidthChanged = (newCharSize.width != oldCharSize.width);
            var charWidth = newCharSize.width;
            if (charWidthChanged) {  
                // if charWidth changed, need to change maxPxPerBp to match
                // console.log("residues font size changed, new char width = " + newCharSize.width);
                if (! browser.config.view) { browser.config.view = {}; }
                browser.config.view.maxPxPerBp = charWidth;
                view.maxPxPerBp = charWidth;
            }
            if (charWidthChanged && fullZoom) {
                // console.log("at full zoom, trying font size fix");
                view.pxPerBp = view.maxPxPerBp;
                view.oldOnResize();
                thisB.browserZoomFix(centerBp);
            }
            else  {
                view.oldOnResize();
            }
        };

        });

        var customGff3Driver = dojo.declare("ApolloGFF3Driver", GFF3Driver,   {
            constructor: function( args ) {
                this.storeType = 'WebApollo/Store/SeqFeature/ApolloGFF3';
            }
        } );
        browser.registerExtraFileDriver(customGff3Driver);
    },


/** 
 *  Hack to try and fix residues rendering bug when web browser scaling/zoom (Cmd+, Cmd-) is used 
 *    bug appears in Chrome, not Firefox, unsure of other browsers
 *    based on GenomeView.zoomToBaseLevel(), GenomeView.updateZoom(), then stripping away unneeded
*/
browserZoomFix: function(pos) {
    var view = this.browser.view;
    if (view.animation) return;
    var baseZoomIndex = view.zoomLevels.length - 1;
    var zoomLoc = 0.5;
    view.showWait();
    view.trimVertical();
    var relativeScale = view.zoomLevels[baseZoomIndex] / view.pxPerBp;
    var fixedBp = pos;
    view.curZoom = baseZoomIndex;
    view.pxPerBp = view.zoomLevels[baseZoomIndex];
    view.maxLeft = (view.pxPerBp * view.ref.end) - view.getWidth();

    // needed, otherwise Density track can render wrong
    //    possibly would have problems with other Canvas-based tracks too, though haven't seen in XYPlot yet
    for (var track = 0; track < view.tracks.length; track++)
	view.tracks[track].startZoom(view.pxPerBp,
				     fixedBp - ((zoomLoc * view.getWidth())
						/ view.pxPerBp),
				     fixedBp + (((1 - zoomLoc) * view.getWidth())
						/ view.pxPerBp));

    var eWidth = view.elem.clientWidth;
    var centerPx = view.bpToPx(fixedBp) - (zoomLoc * eWidth) + (eWidth / 2);
    // stripeWidth: pixels per block
    view.stripeWidth = view.stripeWidthForZoom(view.curZoom);
    view.scrollContainer.style.width =
        (view.stripeCount * view.stripeWidth) + "px";
    view.zoomContainer.style.width =
        (view.stripeCount * view.stripeWidth) + "px";
    var centerStripe = Math.round(centerPx / view.stripeWidth);
    var firstStripe = (centerStripe - ((view.stripeCount) / 2)) | 0;
    view.offset = firstStripe * view.stripeWidth;
    view.maxOffset = view.bpToPx(view.ref.end+1) - view.stripeCount * view.stripeWidth;
    view.maxLeft = view.bpToPx(view.ref.end+1) - view.getWidth();
    view.minLeft = view.bpToPx(view.ref.start);
    view.zoomContainer.style.left = "0px";
    view.setX((centerPx - view.offset) - (eWidth / 2));
    dojo.forEach(view.uiTracks, function(track) { track.clear(); });

    // needed, otherwise Density track can render wrong
    //    possibly would have problems with other Canvas-based tracks too, though haven't seen in XYPlot yet
    view.trackIterate( function(track) {
        track.endZoom( view.pxPerBp,Math.round(view.stripeWidth / view.pxPerBp));
    });

    view.showVisibleBlocks(true);
    view.showDone();
    view.showCoarse();
},


    plusStrandFilter: function(feature)  {
        var strand = feature.get('strand');
        if (strand == 1 || strand == '+')  { return true; }
        else  { return false; }
    },

    minusStrandFilter: function(feature)  {
        var strand = feature.get('strand');
        if (strand == -1 || strand == '-')  { return true; }
        else  { return false; }
    },
    passAllFilter: function(feature)  {  return true; }, 
    passNoneFilter: function(feature)  { return false; }, 

    addStrandFilterOptions: function()  {
        var thisB = this;
        var browser = this.browser;
        var plus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Show plus strand",
                    checked: true,
                    onClick: function(event) {
                        var plus = plus_strand_toggle.checked;
                        var minus = minus_strand_toggle.checked;
                        console.log("plus: ", plus, " minus: ", minus);
                        if (plus && minus)  {
                            browser.setFeatureFilter(thisB.passAllFilter);
                        }
                        else if (plus)  {
                            browser.setFeatureFilter(thisB.plusStrandFilter);
                        }
                        else if (minus)  {
                            browser.setFeatureFilter(thisB.minusStrandFilter);
                        }
                        else  {
                            browser.setFeatureFilter(thisB.passNoneFilter);
                        }
                        browser.view.redrawTracks();
                    }
                });
        browser.addGlobalMenuItem( 'view', plus_strand_toggle );
        var minus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Show minus strand",
                    checked: true,
                    onClick: function(event) {
                        var plus = plus_strand_toggle.checked;
                        var minus = minus_strand_toggle.checked;
                        console.log("plus: ", plus, " minus: ", minus);
                        if (plus && minus)  {
                            browser.setFeatureFilter(thisB.passAllFilter);
                        }
                        else if (plus)  {
                            browser.setFeatureFilter(thisB.plusStrandFilter);
                        }
                        else if (minus)  {
                            browser.setFeatureFilter(thisB.minusStrandFilter);
                        }
                        else  {
                            browser.setFeatureFilter(thisB.passNoneFilter);
                        }
                        browser.view.redrawTracks();
                        }
                });
        browser.addGlobalMenuItem( 'view', minus_strand_toggle );
        browser.addGlobalMenuItem( 'view', new dijitMenuSeparator());
    }, 

/** 
 * hacking addition of a "tools" menu to standard JBrowse menubar, 
 *    with a "Search Sequence" dropdown
 */
    initSearchMenu: function()  {
        if (! this.searchMenuInitialized) { 
            var webapollo = this;
            this.browser.addGlobalMenuItem( 'tools',
                                            new dijitMenuItem(
                                                {
                                                    id: 'menubar_apollo_seqsearch', 
		                                    label: "Search sequence",
		                                    onClick: function() {
		                                        webapollo.getAnnotTrack().searchSequence();
		                                    }
                                                }) );
            this.browser.renderGlobalMenu( 'tools', {text: 'Tools'}, this.browser.menuBar );
        }

        // move Tool menu in front of Help menu (Help should always be last menu?)
        // Dojo weirdness: actual menu pulldown get assigned "widgetid" equal to "id" passed when creating dijit DropDownButton
        var $toolsMenu = $('.menu[widgetid="dropdownbutton_tools"');
        //this.$tools_menu = $('#dropdownbutton_tools').parent().parent();   // gives same result...
        var $helpMenu = $('.menu[widgetid="dropdownbutton_help"');
        $toolsMenu.insertBefore($helpMenu);
        this.searchMenuInitialized = true;
    }, 

    
    initLoginMenu: function(username) {
    	var webapollo = this;
    	var loginButton;
    	if (username)  {   // permission only set if permission request succeeded
    		this.browser.addGlobalMenuItem( 'user',
    				new dijitMenuItem(
    						{
    							label: 'Logout',
    							onClick: function()  {
    								webapollo.getAnnotTrack().logout();
    							}
    						})
    		);
    		var userMenu = this.browser.makeGlobalMenu('user');
    		loginButton = new dijitDropDownButton(
    				{ className: 'user',
    					innerHTML: '<span class="usericon"></span>' + username,
    					title: 'user logged in: UserName',
    					dropDown: userMenu
    				});
    		// if add 'menu' class, button will be placed on left side of menubar instead (because of 'float: left' 
    		//     styling in CSS rule for 'menu' class
    		// dojo.addClass( loginButton.domNode, 'menu' );
    	}
    	else  { 
    		loginButton = new dijitButton(
    				{ className: 'login',
    					innerHTML: "Login",
    					onClick: function()  {
    						webapollo.getAnnotTrack().login();
    					}
    				});
    	}
    	this.browser.menuBar.appendChild( loginButton.domNode );
    	this.loginMenuInitialized = true;
    }, 
    
    /**
     *  get the GenomeView's user annotation track
     *  WebApollo assumes there is only one AnnotTrack
     *     if there are multiple AnnotTracks, getAnnotTrack returns first one found
     *         iterating through tracks list
     */
    getAnnotTrack: function()  {
        if (this.browser && this.browser.view && this.browser.view.tracks)  {
            var tracks = this.browser.view.tracks;
            for (var i = 0; i < tracks.length; i++)  {
	        // should be doing instanceof here, but class setup is not being cooperative
                if (tracks[i].isWebApolloAnnotTrack)  {
                    return tracks[i];
                }
            }
        }
        return null;
    }, 

    /**
     *  get the GenomeView's sequence track
     *  WebApollo assumes there is only one SequenceTrack
     *     if there are multiple SequenceTracks, getSequenceTrack returns first one found
     *         iterating through tracks list
     */
    getSequenceTrack: function()  {
        if (this.browser && this.browser.view && this.browser.view.tracks)  {
            var tracks = this.browser.view.tracks;
            for (var i = 0; i < tracks.length; i++)  {
	        // should be doing instanceof here, but class setup is not being cooperative
                if (tracks[i].isWebApolloSequenceTrack)  {
                    // console.log("seq track refseq: " + tracks[i].refSeq.name);
                    return tracks[i];
                }
            }
        }
        return null;
    }, 
  

    /** ported from berkeleybop/jbrowse GenomeView.js 
      * returns char height/width on GenomeView
      */
    getSequenceCharacterSize: function(recalc)  {
        var container = this.browser.container;
        if (this.browser.view && this.browser.view.elem)  {
            container = this.browser.view.elem;
        }
        if (recalc || (! this._charSize))  {
            //	    this._charSize = this.calculateSequenceCharacterSize(this.browser.view.elem);
	    this._charSize = this.calculateSequenceCharacterSize(container);
        }
        return this._charSize;
    }, 

    /**
     * ported from berkeleybop/jbrowse GenomeView.js 
     * Conducts a test with DOM elements to measure sequence text width
     * and height.
     */
    calculateSequenceCharacterSize: function( containerElement ) {
        var widthTest = document.createElement("div");
        widthTest.className = "wa-sequence";
        widthTest.style.visibility = "hidden";
        var widthText = "12345678901234567890123456789012345678901234567890";
        widthTest.appendChild(document.createTextNode(widthText));
        containerElement.appendChild(widthTest);

        var result = {
            width:  widthTest.clientWidth / widthText.length,
            height: widthTest.clientHeight
        };

        containerElement.removeChild(widthTest);
        return result;
    }, 

    /** utility function, given an array with objects that have label props, 
     *        return array with all objects that don't have label
     *   D = [ { label: A }, { label: B}, { label: C } ]
     *   E = D.removeItemWithLabel("B");
     *   E ==> [ { label: A }, { label: C } ]
     */
    removeItemWithLabel: function(inarray, label) {
        var outarray = [];
        for (var i=0; i<inarray.length; i++) {
            var obj = inarray[i];
            if (! (obj.label && (obj.label === label))) {
                outarray.push(obj);
            }
        }
        return outarray;
    }, 

    setFavicon: function(favurl) {
        var $head = $('head');
        // remove any existing favicons
        var $existing_favs = $("head > link[rel='icon'], head > link[rel='shortcut icon']");
        $existing_favs.remove();

        // add new favicon (as both rel='icon' and rel='shortcut icon' for better browser compatibility)
        var favicon1 = document.createElement('link');
        favicon1.id = "favicon_icon";
        favicon1.rel = 'icon';
        favicon1.type="image/x-icon";
        favicon1.href = favurl;

        var favicon2 = document.createElement('link');
        favicon2.id = "favicon_shortcut_icon";
        favicon2.rel = 'shortcut icon';
        favicon2.type="image/x-icon";
        favicon2.href = favurl;

        $head.prepend(favicon1);
        $head.prepend(favicon2);
    }


});

});

});
