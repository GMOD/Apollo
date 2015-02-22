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
           'dojo/_base/lang',
           'dojo/dom-construct',
           'dojo/dom-class',
           'dojo/_base/window',
           'dojo/_base/array',
           'dojo/request/xhr',
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
           'WebApollo/View/TrackList/Hierarchical',
           'WebApollo/View/TrackList/Faceted',
           'WebApollo/View/Dialog/Help',
           'JBrowse/View/FileDialog/TrackList/GFF3Driver'
       ],
    function( declare,
            lang,
            domConstruct,
            domClass,
            win,
            array,
            xhr,
            dijitMenu,
            dijitMenuItem,
            dijitMenuSeparator,
            dijitCheckedMenuItem,
            dijitPopupMenuItem,
            dijitDropDownButton,
            dijitDropDownMenu,
            dijitButton,
            JBPlugin,
            FeatureEdgeMatchManager,
            FeatureSelectionManager,
            TrackConfigTransformer,
            AnnotTrack,
            Hierarchical,
            Faceted,
            HelpMixin,
            GFF3Driver
            ) {

return declare( [JBPlugin, HelpMixin],
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
        if( browser.cookie("Scheme")=="Dark" ) {
            domClass.add(win.body(), "Dark");
        }
        if( browser.cookie("colorCdsByFrame")=="true" ) {
            domClass.add(win.body(), "colorCds");
        }
        if( browser.config.favicon ) {
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

        this.setupWebapolloTrackTypes();


        thisB.createViewMenu();
        thisB.addStrandFilterOptions();
        thisB.createHelpMenu();
        browser.afterMilestone( 'initView', function() {
            if (browser.poweredByLink)  {
                browser.poweredByLink.innerHTML = '<img src=\"plugins/WebApollo/img/ApolloLogo_100x36.png\" height=\"25\" />';
            }
            var help=dijit.byId("menubar_generalhelp");
            help.set("label", "Web Apollo Help");

        });
        this.monkeyPatchRegexPlugin();

    },
    showLabels: function(show,updating) {
        var browser=this.browser;
        var showLabels;
        if(updating) {
            showLabels=(browser.cookie("showTrackLabel")||"true")=="true";
        }
        else { showLabels=show; }
        if(!showLabels) {
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
            if(browser.cookie(name)=="true") {
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
                    checked: browser.cookie("plusStrandFilter")=="true",
                    onClick: function(event) {
                        browser.cookie("plusStrandFilter",this.get("checked")?"true":"false");
                        strandFilter("plusStrandFilter",plusStrandFilter);
                        browser.view.redrawTracks();
                    }
                });
        var minus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Hide minus strand",
                    checked: browser.cookie("minusStandFilter")=="true",
                    onClick: function(event) {
                        browser.cookie("minusStrandFilter",this.get("checked")?"true":"false");
                        strandFilter("minusStrandFilter",minusStrandFilter);
                        browser.view.redrawTracks();
                    }
                });
        browser.addGlobalMenuItem( 'view', minus_strand_toggle );
        browser.addGlobalMenuItem( 'view', plus_strand_toggle );

        strandFilter("minusStrandFilter",minusStrandFilter);
        strandFilter("plusStrandFilter",plusStrandFilter);
    },
    
    
        
    createNavigationOptions: function()  {
        var browser = this.browser;
        var select_Tracks = new dijitMenuItem(
            {
                label: "Sequences",
                onClick: function(event) {
                    window.open('../sequences', '_blank');
                }
            });
        browser.addGlobalMenuItem( 'tools', select_Tracks );
        var recent_Changes = new dijitMenuItem(
            {
                label: "Changes",
                onClick: function(event) {
                    window.open('../changes', '_blank');
                }
            });
        browser.addGlobalMenuItem( 'tools', recent_Changes );
    },

    initSearchMenu: function()  {
        var thisB = this;
        this.browser.addGlobalMenuItem( 'tools',
            new dijitMenuItem(
                {
                    id: 'menubar_apollo_seqsearch',
                    label: "Search sequence",
                    onClick: function() {
                        thisB.getAnnotTrack().searchSequence();
                    }
                })
        );
        createNavigationOptions();
        this.browser.renderGlobalMenu( 'tools', {text: 'Tools'}, this.browser.menuBar );

        // move Tool menu in front of Help menu
        var toolsMenu = dijit.byId('dropdownbutton_tools');
        var helpMenu = dijit.byId('dropdownbutton_help');
        domConstruct.place(toolsMenu.domNode,helpMenu.domNode,'before');
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
            var a;
            array.some(this.browser.view.tracks,function(track) {
                if(track.isInstanceOf(AnnotTrack))  {
                    a=track;
                    return true;
                }
            });
            return a;
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
            var a;
            array.some(this.browser.view.tracks,function(track) {
                if (track.isInstanceOf(SequenceTrack))  {
                    a=track;
                    return true;
                }
            });
            return a;
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
    createViewMenu: function() {
        var browser=this.browser;
        var thisB=this;

        // add a global menu option for setting CDS color
        var cds_frame_toggle = new dijitCheckedMenuItem(
                {
                    label: "Color by CDS frame",
                    checked: browser.cookie("colorCdsByFrame")=="true",
                    onClick: function(event) {
                        if(this.get("checked")) domClass.add(win.body(), "colorCds");
                        else domClass.remove(win.body(),"colorCds");
                        browser.cookie("colorCdsByFrame", this.get("checked")?"true":"false");
                    }
                });
        browser.addGlobalMenuItem( 'view', cds_frame_toggle );

        var css_frame_menu = new dijitMenu();

        css_frame_menu.addChild(
            new dijitMenuItem({
                    label: "Light",
                    onClick: function (event) {
                        browser.cookie("Scheme","Light");
                        domClass.remove(win.body(), "Dark");
                    }
                }
            )
        );
        css_frame_menu.addChild(
            new dijitMenuItem({
                    label: "Dark",
                    onClick: function (event) {
                        browser.cookie("Scheme","Dark");
                        domClass.add(win.body(), "Dark");
                    }
                }
            )
        );


        var css_frame_toggle = new dijitPopupMenuItem(
            {
                label: "Color Scheme",
                popup: css_frame_menu
            });

        browser.addGlobalMenuItem('view', css_frame_toggle);

        var hide_track_label_toggle = new dijitCheckedMenuItem(
            {
                label: "Show track label",
                checked: (browser.cookie("showTrackLabel")||"true")=="true",
                onClick: function(event) {
                    thisB.showLabels(this.get("checked"));
                    browser.cookie("showTrackLabel",this.get("checked")?"true":"false");
                }
            });

        this.showLabels();
        browser.subscribe('/jbrowse/v1/n/tracks/visibleChanged', dojo.hitch(this,"showLabels",true));


        browser.addGlobalMenuItem( 'view', hide_track_label_toggle);
        browser.addGlobalMenuItem( 'view', new dijitMenuSeparator());
    },


    createHelpMenu: function() {
        var browser=this.browser;
        if( !browser.config.quickHelp ) {
            browser.config.quickHelp = {
                "title": "Web Apollo Help",
                "content": this.defaultHelp()
            }
        }
        var jbrowseUrl = "http://jbrowse.org";
        var browser=this.browser;
        
        browser.addGlobalMenuItem( 'help',
            new dijitMenuItem(
                {
                    id: 'menubar_powered_by_jbrowse',
                    label: 'Powered by JBrowse',
                    onClick: function()  { window.open(jbrowseUrl,'help_window').focus(); }
                })
        );
        browser.addGlobalMenuItem( 'help',
            new dijitMenuItem(
                {
                    id: 'menubar_web_service_api',
                    label: 'Web Service API',
                    onClick: function()  { window.open("../web_services/web_service_api.html",'help_window').focus(); }
                })
        );
        browser.addGlobalMenuItem( 'help',
            new dijitMenuItem(
                {
                    id: 'menubar_apollo_version',
                    label: 'Get Version',
                    onClick: function()  {
                        window.open("../version.jsp",'help_window').focus();
                    }
                })
        );

    },


    monkeyPatchRegexPlugin: function() {
        var plugin='RegexSequenceSearch/Store/SeqFeature/RegexSearch';
        require([plugin], function(RegexSearch) {
            lang.extend(RegexSearch,{
                translateSequence:function( sequence, frameOffset ) {
                    var slicedSeq = sequence.slice( frameOffset );
                    slicedSeq = slicedSeq.slice( 0, Math.floor( slicedSeq.length / 3 ) * 3);

                    var translated = "";
                    var codontable=new CodonTable();
                    var codons=codontable.generateCodonTable(codontable.defaultCodonTable);
                    for(var i = 0; i < slicedSeq.length; i += 3) {
                        var nextCodon = slicedSeq.slice(i, i + 3);
                        translated = translated + codons[nextCodon];
                    }

                    return translated;
                }
            });
        });
     },
     setupWebapolloTrackTypes: function() {
        var browser=this.browser;
        var thisB=this;

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
            defaultForStoreTypes: [ 'JBrowse/Store/SeqFeature/BAM' ],
            label: 'WebApollo Alignments'
        });
        browser.registerTrackType({
            type:                 'WebApollo/View/Track/SequenceTrack',
            defaultForStoreTypes: [ 'JBrowse/Store/Sequence/StaticChunked' ],
            label: 'WebApollo Sequence'
        });

        // transform track configs from vanilla JBrowse to WebApollo:
        // type: "JBrowse/View/Track/HTMLFeatures" ==> "WebApollo/View/Track/DraggableHTMLFeatures"
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


     }


});

});

});
