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
        this.inherited(arguments);
        console.log("WebApollo Combination");
    }
});
});
