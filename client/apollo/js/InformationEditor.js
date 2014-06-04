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
return declare( 'WebApollo.View.InformationEditor', null,
   /** 
    * @lends WebApollo.View.InformationEditor
    */  
   {   

   /** 
     * Track selector with facets and text searching.
     * @constructs
     */  
   constructor: function(args) {
        console.log('Test panel');
        var topPane = new ContentPane({ className: 'header' }); 
        this.addChild( topPane );
        dom.create(
            'h2',
            { className: 'title',
              innerHTML: 'Information Editor'
            },  
            topPane.contentNode ); 
   }
});

});
