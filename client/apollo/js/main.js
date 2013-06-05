require({
           packages: [
               { name: 'jqueryui', location: '../plugins/WebApollo/jslib/jqueryui' },
               { name: 'jquery', location: '../plugins/WebApollo/jslib/jquery', main: 'jquery' }
           ]
       },
       [],
       function() {

define.amd.jQuery = true;

define(
       [
           'dojo/_base/declare',
           'dijit/MenuItem', 
           'dijit/CheckedMenuItem',
           'dijit/form/DropDownButton',
           'dijit/DropDownMenu',
           'dijit/form/Button',
           'JBrowse/Plugin',
           './FeatureEdgeMatchManager',
	   './FeatureSelectionManager',
	   './View/Track/AnnotTrack'
       ],
    function( declare, dijitMenuItem, dijitCheckedMenuItem, dijitDropDownButton, dijitDropDownMenu, dijitButton, JBPlugin, 
              FeatureEdgeMatchManager, FeatureSelectionManager, AnnotTrack ) {

return declare( JBPlugin,
{
//    colorCdsByFrame: false,
//    searchMenuInitialized: false,

    constructor: function( args ) {
        var thisB = this;
        this.colorCdsByFrame = false;
        this.searchMenuInitialized = false;
        var browser = this.browser;  // this.browser set in Plugin superclass constructor

        if (! browser.config.helpUrl)  {
	    browser.config.helpUrl = "http://genomearchitect.org/webapollo/docs/help.html";
        }

        // hand the browser object to the feature edge match manager
        FeatureEdgeMatchManager.setBrowser( browser );

	this.featSelectionManager = new FeatureSelectionManager();
	this.annotSelectionManager = new FeatureSelectionManager();

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
        browser.addGlobalMenuItem( 'options', cds_frame_toggle );
        var plus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Show plus strand",
                    checked: true,
                    onClick: function(event) {
                        var plus = plus_strand_toggle.checked;
                        var minus = minus_strand_toggle.checked;
                        console.log("plus: ", plus, " minus: ", minus);
                        if (plus && minus)  {
                            browser.view.featureFilter = browser.view.passAllFilter;
                        }
                        else if (plus)  {
                            browser.view.featureFilter = browser.view.plusStrandFilter;
                        }
                        else if (minus)  {
                            browser.view.featureFilter = browser.view.minusStrandFilter;
                        }
                        else  {
                            browser.view.featureFilter = browser.view.passNoneFilter;
                        }
                        // browser.view.redrawTracks();
                        thisB.redoLayout();
                    }
                });
        browser.addGlobalMenuItem( 'options', plus_strand_toggle );
        var minus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Show minus strand",
                    checked: true,
                    onClick: function(event) {
                        var plus = plus_strand_toggle.checked;
                        var minus = minus_strand_toggle.checked;
                        console.log("plus: ", plus, " minus: ", minus);
                        if (plus && minus)  {
                            browser.view.featureFilter = browser.view.passAllFilter;
                        }
                        else if (plus)  {
                            browser.view.featureFilter = browser.view.plusStrandFilter;
                        }
                        else if (minus)  {
                            browser.view.featureFilter = browser.view.minusStrandFilter;
                        }
                        else  {
                            browser.view.featureFilter = browser.view.passNoneFilter;
                        }
                        // browser.view.redrawTracks();
                        thisB.redoLayout();
                    }
                });
        browser.addGlobalMenuItem( 'options', minus_strand_toggle );

        // register the WebApollo track types with the browser, so
        // that the open-file dialog and other things will have them
        // as options
        browser.registerTrackType({
            type:                 'WebApollo/View/Track/DraggableHTMLFeatures',
            defaultForStoreTypes: [ 'JBrowse/Store/SeqFeature/NCList',
                                    'JBrowse/Store/SeqFeature/GFF3'
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
            if (track_config.type ==  "JBrowse/View/Track/HTMLFeatures" )  { 
                track_config.type = "WebApollo/View/Track/DraggableHTMLFeatures"; 
                console.log("changed track type for " + track_config.label);
            }
        }


        // put the WebApollo logo in the powered_by place in the main JBrowse bar
        browser.afterMilestone( 'initView', function() {
            browser.poweredByLink.innerHTML = '<img src=\"plugins/WebApollo/img/ApolloLogo_100x36.png\" height=\"25\" />';
            browser.poweredByLink.href = 'http://www.gmod.org/wiki/WebApollo';
            browser.poweredByLink.target = "_blank";
        });

    },

    // would rather call view.redrawTracks()
    //
    // BUT, view.redrawTracks currently doesn't force relayout
    //     browser.view.redrawTracks();
    // track.changed() forces relayout (at least for HTMLFeatures)
    //    but also call changeCallBack(), which currently is always view.showVisibleBlocks()
    //    thus will needlessly call view.showVisibleBlocks() repeatedly 
    // so trying for now to be explicit
    redoLayout: function()  {
        this.browser.view.trackIterate( function(t) { 
                                       t.hideAll(); 
                                       if (t._clearLayout)  { 
                                           // console.log("clearing layout for track: " + t.label);
                                           t._clearLayout(); 
                                       } 
                                   } 
                                 );
        this.browser.view.showVisibleBlocks(true);
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
		                                    label: "Search sequence",
		                                    onClick: function() {
		                                        webapollo.getAnnotTrack().searchSequence();
		                                    }
                                                }) );
            var toolMenu = this.browser.makeGlobalMenu('tools');
            if( toolMenu ) {
                var toolButton = new dijitDropDownButton(
                    { className: 'file',
                      innerHTML: 'Tools',
                      //title: '',
                      dropDown: toolMenu
                    });
                dojo.addClass( toolButton.domNode, 'menu' );
                this.browser.menuBar.appendChild( toolButton.domNode );
            }
        }
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
    
    getAnnotTrack: function()  {
        if (this.browser && this.browser.view && this.browser.view.tracks)  {
            var tracks = this.browser.view.tracks;
            for (var i = 0; i < tracks.length; i++)  {
	        // should be doing instanceof here, but class setup is not being cooperative
                if (tracks[i].isWebApolloAnnotTrack)  {
                    console.log("annot track refseq: " + tracks[i].refSeq.name);
                    return tracks[i];
                }
            }
        }
        return null;
    }

});

});

});