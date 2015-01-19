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
           'dojo/dom-construct',
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
           'WebApollo/InformationEditor',
           'JBrowse/View/FileDialog/TrackList/GFF3Driver',
           'lazyload/lazyload'
       ],
    function( declare, domConstruct, xhr, dijitMenu,dijitMenuItem, dijitMenuSeparator, dijitCheckedMenuItem, dijitPopupMenuItem, dijitDropDownButton, dijitDropDownMenu, dijitButton, JBPlugin,
              FeatureEdgeMatchManager, FeatureSelectionManager, TrackConfigTransformer, AnnotTrack, Hierarchical, Faceted, InformationEditor, GFF3Driver,LazyLoad ) {

return declare( JBPlugin,
{

    constructor: function( args ) {
        console.log("loaded WebApollo plugin");
        var thisB = this;
        this.colorCdsByFrame = false;
        this.searchMenuInitialized = false;
        this.showTrackLabel = true ;
        var browser = this.browser;  // this.browser set in Plugin superclass constructor
        [
          'plugins/WebApollo/jslib/bbop/bbop.js',
          'plugins/WebApollo/jslib/bbop/golr.js',
          'plugins/WebApollo/jslib/bbop/jquery.js',
          'plugins/WebApollo/jslib/bbop/search_box.js'
        ].forEach(function(src) {
          var script = document.createElement('script');
          script.src = src;
          script.async = false;
          document.head.appendChild(script);
        });

        // Checking for cookie for determining the color scheme of WebApollo
        if (browser.cookie.indexOf("Scheme=Dark") === -1) {
            this.changeCssScheme = false;
        }
        else {
            this.changeCssScheme = true;
            LazyLoad.css('plugins/WebApollo/css/maker_darkbackground.css');
        }


        if (browser.config.favicon) {
            this.setFavicon(browser.config.favicon);
        }

        args.cssLoaded.then( function() {
            if (! browser.config.view) { console.log("View not loaded"); }
            browser.config.view.maxPxPerBp = 30;
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

        this.addNavigationOptions();

        this.addStrandFilterOptions();


        if (browser.config.show_nav) {
            this.createMenuBar();
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
        browser.config.tracks.forEach(function(track) { thisB.trackTransformer.transform(track) });

        // update track selector to WebApollo's if needed
        // if no track selector set, use WebApollo's Hierarchical selector
        if (!browser.config.trackSelector) {
            browser.config.trackSelector = { type: 'WebApollo/View/TrackList/Hierarchical' };
        }
        // if using JBrowse's Hierarchical selector, switch to WebApollo's
        else if (browser.config.trackSelector.type == "Hierarchical") {
            browser.config.trackSelector.type = 'WebApollo/View/TrackList/Hierarchical';
        }
        // if using JBrowse's Hierarchical selector, switch to WebApollo's
        else if (browser.config.trackSelector.type == "Faceted") {
            browser.config.trackSelector.type = 'WebApollo/View/TrackList/Faceted';
        }

        // put the WebApollo logo in the powered_by place in the main JBrowse bar
        browser.afterMilestone( 'initView', function() {
            if (browser.poweredByLink)  {
                browser.poweredByLink.innerHTML = '<img src=\"plugins/WebApollo/img/ApolloLogo_100x36.png\" height=\"25\" />';
                browser.poweredByLink.href = 'http://genomearchitect.org/';
                browser.poweredByLink.target = "_blank";
            }
            
            var view = browser.view;
            

            var customGff3Driver = dojo.declare("ApolloGFF3Driver", GFF3Driver,   {
                constructor: function( args ) {
                    this.storeType = 'WebApollo/Store/SeqFeature/ApolloGFF3';
                }
            });
            browser.fileDialog.addFileTypeDriver(new customGff3Driver());

        });
    },



    plusStrandFilter: function(feature)  {
        var strand = feature.get('strand');
        return strand == 1 || strand == '+';
    },

    minusStrandFilter: function(feature)  {
        var strand = feature.get('strand');
        return strand == -1 || strand == '-';
    },
    passAllFilter: function(feature)  {  return true; },
    passNoneFilter: function(feature)  { return false; },
    processFilters: function(plus,minus) {
        var plus = plus_strand_toggle.checked;
        var minus = minus_strand_toggle.checked;
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
    },

    addStrandFilterOptions: function()  {
        var thisB = this;
        var browser = this.browser;
        var plus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Show plus strand",
                    checked: true,
                    onClick: function(event) {
                        processFilter(plus_strand_toggle.checked,minus_strand_toggle.checked);
                    }
                });
        browser.addGlobalMenuItem( 'view', plus_strand_toggle );
        var minus_strand_toggle = new dijitCheckedMenuItem(
                {
                    label: "Show minus strand",
                    checked: true,
                    onClick: function(event) {
                        processFilter(plus_strand_toggle.checked,minus_strand_toggle.checked);
                    }
                });
        browser.addGlobalMenuItem( 'view', minus_strand_toggle );
        var hide_track_label_toggle = new dijitCheckedMenuItem(
            {
                label: "Show track label",
                checked: this.showTrackLabel,
                onClick: function(event) {
                    if(hide_track_label_toggle.checked){
                        $('.track-label').show();
                        this.showTrackLabel = true ;
                    }
                    else{
                        $('.track-label').hide();
                        this.showTrackLabel = false ;
                    }
                }
            });

        if(this.showTrackLabel){
            $('.track-label').show();
        }
        else{
            $('.track-label').hide();
        }

        browser.addGlobalMenuItem( 'view', hide_track_label_toggle);
        browser.addGlobalMenuItem( 'view', new dijitMenuSeparator());
    },
    addNavigationOptions: function()  {
        var thisB = this;
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
        this.browser.renderGlobalMenu( 'tools', {text: 'Tools'}, this.browser.menuBar );

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
                { 
                        className: 'user',
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
                    return tracks[i];
                }
            }
        }
        return null;
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
    },


    createMenuBar: function() {
        var browser=this.browser;
        var jbrowseUrl = "http://jbrowse.org";
        var thisB = this;

        browser.addGlobalMenuItem( 'help',
            new dijitMenuItem(
                {
                    id: 'menubar_web_service_api',
                    label: 'Web Service API',
                    // iconClass: 'jbrowseIconHelp',
                    onClick: function()  { window.open("../web_services/web_service_api.html",'help_window').focus(); }
                })
        );
        browser.addGlobalMenuItem( 'help',
            new dijitMenuItem(
                {
                    id: 'menubar_apollo_users_guide',
                    label: 'Apollo User\'s Guide',
                    // iconClass: 'jbrowseIconHelp',
                    onClick: function()  {
                        window.open("http://genomearchitect.org/web_apollo_user_guide",'help_window').focus();
                    }
                })
        );
        xhr("../version.jsp",function(content) {
            console.log("Loading version");
            thisB.browser.version=function() {
                return content;
            };
        });
        
        
        // add a global menu option for setting CDS color
        browser.addGlobalMenuItem( 'view',
            new dijitCheckedMenuItem(
                {
                    label: "Color by CDS frame",
                    checked: false,
                    onChange: function(checked) {
                        console.log(checked);
                        thisB.colorCdsByFrame = checked;
                        browser.view.redrawTracks();
                    }
                })
         );


        // add light/dark choice
        var css_frame_menu = new dijitMenu();

        css_frame_menu.addChild(
            new dijitMenuItem({
                    label: "Light",
                    onClick: function (event) {
                        browser.cookie = "Scheme=Light";
                        window.location.reload();
                    }
                }
            )
        );
        css_frame_menu.addChild(
            new dijitMenuItem({
                    label: "Dark",
                    onClick: function (event) {
                        browser.cookie = "Scheme=Dark";
                        window.location.reload();
                    }
                }
            )
        );
        browser.addGlobalMenuItem('view',
            new dijitPopupMenuItem(
            {
                label: "Color Scheme"
                ,popup: css_frame_menu
            })
        );
   }


});

});

});
