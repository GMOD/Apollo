define(
    [   
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'dijit/TitlePane',
        'dijit/layout/ContentPane',
        'JBrowse/Util',
        'dojo/dom-construct',
        'JBrowse/View/TrackList/_TextFilterMixin'
    ],  
    function (
        declare,
        array,
        lang,
        TitlePane,
        ContentPane,
        Util,
        dom,
        _TextFilterMixin
    ) { 

var dojof = Util.dojof;
return declare(
        'WebApollo.View.InformationEditor',
        [ ContentPane, _TextFilterMixin ],
{
    region: 'left',
    splitter: true,
    style: 'width: 25%',

    id: 'informationEditor',
    baseClass: 'webapolloInformationEditor',

    /** 
      * Track selector with facets and text searching.
      * @constructs
      */  
    constructor: function() {
        console.log('Testing InformationEditor');
    },

    postCreate: function() {
        console.log('Testing InformationEditor postCreate');
        this.placeAt( this.browser.container );
    },
    buildRendering: function() {
        this.inherited(arguments);
       
        var topPane = new ContentPane({ className: 'header' }); 
        this.addChild( topPane );
        dom.create(
            'h2',
            { className: 'title',
              innerHTML: 'Annotation Information Editor'
            },
            topPane.containerNode );

        this._makeTextFilterNodes(
            dom.create('div',
                { className: 'textfilterContainer' },
            topPane.containerNode )
        );  
        this._updateTextFilterControl();

        this.informationList =
        { pane: new ContentPane({ id: 'informationEditorList', className: 'information_list' }).placeAt( this.containerNode )
        };
    }
});

});
