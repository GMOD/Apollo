define([
        "dojo/_base/declare",
        "JBrowse/View/Track/Combination"
       ], 
       function(
        declare,
        Combination
       )
{

return declare(Combination, {
    constructor: function() {
        var thisB=this;
        this.inherited(arguments);
        this.trackClasses.set.resultsTypes.push({
                     name: "DraggableHTMLFeatures",
                     path: "WebApollo/View/Track/DraggableHTMLFeatures"
                 });
        this.supportedBy["WebApollo/View/Track/DraggableHTMLFeatures"]="set";
        console.log("WebApollo Combination Track loaded");
        this.config.fmtMetaValue_Description= function() {
            console.log("Testing");
            return thisB._generateTreeFormula(thisB.opTree);
        }
    }
});
});
