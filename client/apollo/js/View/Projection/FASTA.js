define([
           'dojo/_base/declare',
           'dojo/dom-construct',
           'JBrowse/View/FASTA',
           'dijit/Toolbar',
           'dijit/form/Button',
           'JBrowse/Util',
           'JBrowse/has'
       ],
       function( declare, dom,FASTA, Toolbar, Button, Util, has ) {

return declare(FASTA,
{

    _defaultConfig: function() {
        return Util.deepUpdate(
            dojo.clone( this.inherited(arguments) ));
    },
    constructor: function( args ) {
        // this.width       = args.width || 78;
        // this.htmlMaxRows = args.htmlMaxRows || 15;
        // this.track = args.track;
        // this.canSaveFiles = args.track &&  args.track._canSaveFiles && args.track._canSaveFiles();
    },
    getApollo: function(){
        return window.parent;
    },

    renderText: function( region, seq ) {
        // this is usually the name
        var refSeqString = region.ref + ' '+Util.assembleLocString(region);
        if(refSeqString.indexOf("{")>=0){
            // if it doesn't end with it
            var splitIndex = region.ref.lastIndexOf(":");
            var refSeqObject = JSON.parse(region.ref.substr(0,splitIndex));
            refSeqString = Util.renderRefSeqName(refSeqObject) ;
            // var locationString = region.ref.substr(splitIndex);
            // refSeqString += " "+locationString;
            // refSeqString = "***123 projected**** START...FINISH";
            refSeqString += ":"+region.start +".."+region.end;
        }

        return '>' + refSeqString
            + ( region.type ? ' class='+region.type : '' )
            + ' length='+(region.end - region.start)
            + "\n"
            + this._wrap( seq, this.width );
    },
    _wrap: function( string, length ) {
        length = length || this.width;
        return string.replace( new RegExp('(.{'+length+'})','g'), "$1\n" );
    }
});
});
