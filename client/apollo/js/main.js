require({
           packages: [
               { name: 'jqueryui', location: '../plugins/WebApollo/jslib/jqueryui' },
               { name: 'jquery', location: '../plugins/WebApollo/jslib/jquery', main: 'jquery' }
           ]
       },
       [],
       function() {
define.amd.jQuery = true;
define([
           'dojo/_base/declare',
           'dojo/dom-construct',
           'dojo/_base/array',
           'dijit/Menu',
           'dijit/MenuItem',
           'dijit/MenuSeparator',
           'dijit/CheckedMenuItem',
           'dijit/PopupMenuItem',
           'dijit/form/DropDownButton',
           'dijit/DropDownMenu',
           'dijit/form/Button',
           'JBrowse/Plugin',
           'WebApollo/FeatureEdgeMatchManager',
           'WebApollo/FeatureSelectionManager',
           'WebApollo/TrackConfigTransformer',
           'WebApollo/View/Track/AnnotTrack',
           'WebApollo/View/Track/SequenceTrack',
           'WebApollo/View/TrackList/Hierarchical',
           'WebApollo/View/TrackList/Faceted',
           'WebApollo/InformationEditor',
           'JBrowse/View/FileDialog/TrackList/GFF3Driver',
           'lazyload/lazyload'
       ],
    function( declare, domConstruct, array, dijitMenu,dijitMenuItem, dijitMenuSeparator, dijitCheckedMenuItem, dijitPopupMenuItem, dijitDropDownButton, dijitDropDownMenu, dijitButton, JBPlugin,
              FeatureEdgeMatchManager, FeatureSelectionManager, TrackConfigTransformer, AnnotTrack, SequenceTrack, Hierarchical, Faceted, InformationEditor, GFF3Driver,LazyLoad ) {


return declare( JBPlugin,
{

    constructor: function( args ) {
        console.log("loaded WebApollo plugin");
        var thisB = this;
        this.searchMenuInitialized = false;
        var browser = this.browser;  // this.browser set in Plugin superclass constructor
        var externals=[
          'plugins/WebApollo/jslib/bbop/bbop.js',
          'plugins/WebApollo/jslib/bbop/golr.js',
          'plugins/WebApollo/jslib/bbop/jquery.js',
          'plugins/WebApollo/jslib/bbop/search_box.js'
        ];
        array.forEach(externals,function(src) {
          var script = document.createElement('script');
          script.src = src;
          script.async = false;
          document.head.appendChild(script);
        });

        // Checking for cookie for determining the color scheme of WebApollo
        if (browser.cookie("Scheme")=="Dark") {
            LazyLoad.css('plugins/WebApollo/css/maker_darkbackground.css');
        }

        browser.subscribe('/jbrowse/v1/n/tracks/visibleChanged', dojo.hitch(this,"updateLabels"));




        if (browser.config.favicon) {
            this.setFavicon(browser.config.favicon);
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


        if(!browser.config.aboutThisBrowser) {
            browser.config.aboutThisBrowser={
                description:"This is WebApollo 1.0.4",
                title:"WebApollo 1.0.4"
            };
        }
        if(!browser.config.quickHelp) {
            browser.config.quickHelp={
                content:"This is the help guide",
                title:"Help guide"
            };
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
                                    'JBrowse/Store/SeqFeature/BAM'
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
        array.forEach(browser.config.tracks,function(e) { thisB.trackTransformer.transform(e); });

        if (!browser.config.trackSelector) {
            browser.config.trackSelector = { type: 'WebApollo/View/TrackList/Hierarchical' };
        }
        else if (browser.config.trackSelector.type == "Hierarchical") {
            browser.config.trackSelector.type = 'WebApollo/View/TrackList/Hierarchical';
        }
        else if (browser.config.trackSelector.type == "Faceted") {
            browser.config.trackSelector.type = 'WebApollo/View/TrackList/Faceted';
        }

        // put the WebApollo logo in the powered_by place in the main JBrowse bar
        browser.afterMilestone( 'initView', function() {
            if (browser.poweredByLink)  {
                browser.poweredByLink.innerHTML = '<img src=\"plugins/WebApollo/img/ApolloLogo_100x36.png\" height=\"25\" />';
            }

            // Initialize information editor with similar style to track selector
            browser.fileDialog.addFileTypeDriver(new GFF3Driver());
        });
    },
    updateLabels: function() {
        var browser=this.browser;
        if(browser.cookie("showTrackLabel")=="0") {
            $('.track-label').hide();
        }
        else {
            $('.track-label').show();
        }
    },

    addStrandFilterOptions: function()  {
        var browser=this.browser;
        var thisB = this;

        var strandFilter = function(name,callback) {
            if(browser.cookie(name)=="1") {
                browser.addFeatureFilter(callback,name);
            } else {
                browser.removeFeatureFilter(name);
            }
        };
        var minusStrandFilter = function(feature)  {
            var strand = feature.get('strand');
            return strand == 1 || strand == '+';
        };

        var plusStrandFilter = function(feature)  {
            var strand = feature.get('strand');
            return strand == -1 || strand == '-';
        };

        var plus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Hide plus strand",
                    checked: browser.cookie("plusStrandFilter")=="1",
                    onClick: function(event) {
                        browser.cookie("plusStrandFilter",this.get("checked")?"1":"0");
                        thisB.strandFilter("plusStrandFilter",thisB.plusStrandFilter);
                        browser.view.redrawTracks();
                    }
                });
        var minus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Hide minus strand",
                    checked: browser.cookie("minusStandFilter")=="1",
                    onClick: function(event) {
                        browser.cookie("minusStrandFilter",this.get("checked")?"1":"0");
                        thisB.strandFilter("minusStrandFilter",thisB.minusStrandFilter);
                        browser.view.redrawTracks();
                    }
                });
        browser.addGlobalMenuItem( 'view', minus_strand_toggle );
        browser.addGlobalMenuItem( 'view', plus_strand_toggle );

        this.strandFilter("minusStrandFilter",this.minusStrandFilter);
        this.strandFilter("plusStrandFilter",this.plusStrandFilter);
    },
    
    
        
    addNavigationOptions: function()  {
        var browser = this.browser;
        var select_Tracks = new dijitMenuItem(
            {
                label: "Sequences",
                onClick: function(event) {
                    window.open('../sequences', '_blank');
                }
            });
        browser.addGlobalMenuItem( 'view', select_Tracks );
        var recent_Changes = new dijitMenuItem(
            {
                label: "Changes",
                onClick: function(event) {
                    window.open('../changes', '_blank');
                }
            });
        browser.addGlobalMenuItem( 'view', recent_Changes );
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

        // move Tool menu in front of Help menu
        var toolsMenu = dojo.query('.menu[widgetid="dropdownbutton_tools"]')[0];
        var helpMenu = dojo.query('.menu[widgetid="dropdownbutton_help"]')[0];
        domConstruct.place(toolsMenu,helpMenu,'before');
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
            array.some(this.browser.view.tracks,function(track) {
                if (track.isInstanceOf(AnnotTrack))  {
                    return track;
                }
            });
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
            array.some(tracks,function(track) {
                if (track.isInstanceOf(SequenceTrack))  {
                    return track;
                }
            });
        }
        return null;
    },

    removeItemWithLabel: function(inarray, label) {
        return array.filter(inarray,function(obj) {
            return ! (obj.label && (obj.label === label));
        });
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
    },
    createMenu: function() {
        this.addNavigationOptions();

        // add a global menu option for setting CDS color
        var cds_frame_toggle = new dijitCheckedMenuItem(
                {
                    label: "Color by CDS frame",
                    checked: browser.cookie("colorCdsByFrame")=="1",
                    onClick: function(event) {
                        browser.cookie("colorCdsByFrame", this.get("checked")?"1":"0");
                        browser.view.redrawTracks();
                    }
                });
        browser.addGlobalMenuItem( 'view', cds_frame_toggle );

        var css_frame_menu = new dijitMenu();

        css_frame_menu.addChild(
            new dijitMenuItem({
                    label: "Light",
                    onClick: function (event) {
                        browser.cookie("Scheme","Light");
                        window.location.reload();
                    }
                }
            )
        );
        css_frame_menu.addChild(
            new dijitMenuItem({
                    label: "Dark",
                    onClick: function (event) {
                        browser.cookie("Scheme","Dark");
                        window.location.reload();
                    }
                }
            )
        );


        var css_frame_toggle = new dijitPopupMenuItem(
            {
                label: "Color Scheme"
                ,popup: css_frame_menu
            });

        browser.addGlobalMenuItem('view', css_frame_toggle);

        this.addStrandFilterOptions();
        var hide_track_label_toggle = new dijitCheckedMenuItem(
            {
                label: "Show track label",
                checked: (browser.cookie("showTrackLabel")||"1")=="1",
                onClick: function(event) {
                    browser.cookie("showTrackLabel",this.get("checked")?"1":"0");
                    thisB.updateLabels();
                }
            });
        this.updateLabels();
        browser.addGlobalMenuItem( 'view', hide_track_label_toggle);
        browser.addGlobalMenuItem( 'view', new dijitMenuSeparator());


    }


});

});

});


