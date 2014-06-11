define(
    [   
        'dojo/_base/declare',
        'dojo/_base/array',
        'dojo/_base/lang',
        'dijit/TitlePane',
        'dijit/layout/ContentPane',
        'JBrowse/Util',
        'dojo/dom-construct'
    ],  
    function (
        declare,
        array,
        lang,
        TitlePane,
        ContentPane,
        Util,
        dom
    ) { 

var dojof = Util.dojof;
return declare( 'WebApollo.View.InformationEditor', [ ContentPane ],
{
    region: 'left',
    splitter: true,
    style: 'width: 25%',

    id: 'informationEditor',
    baseClass: 'webapolloInformationEditor',
    title: 'Info Editor'
},

   /** 
    * @lends WebApollo.View.InformationEditor
    */  
   {   

   /** 
     * Track selector with facets and text searching.
     * @constructs
     */  
   constructor: function(args) {
       var topPane = new ContentPane({ className: 'header' }); 
       this.addChild( topPane );
       dom.create(
           'h2',
           { className: 'title',
             innerHTML: 'Annotation Information Editor'
           },  
           topPane.containerNode );
       
       var contentPane = new ContentPane({ className: 'uncategorized', id: 'informationEditorContent' }); 
       this.addChild( this.contentPane );
   }
});

});
