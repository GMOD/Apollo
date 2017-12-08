define( [
        'dojo/_base/declare',
        'dojo/_base/array',
        'JBrowse/has',
        'JBrowse/Util',
        'JBrowse/Errors',
        'JBrowse/Store/LRUCache',
        'WebApollo/Store/SeqFeature/BAM/LazyFeature',
        'JBrowse/Store/SeqFeature/BAM/Util',
        'JBrowse/Store/SeqFeature/BAM/File'
    ],
    function(
        declare,
        array,
        has,
        Util,
        Errors,
        LRUCache,
        WebApolloBAMFeature,
        BAMUtil,
        JBrowseBAMFile
    ) {

    var readInt   = BAMUtil.readInt;
    var readVirtualOffset = BAMUtil.readVirtualOffset;

    var WebApolloBAMFile = declare( JBrowseBAMFile, {

        /*
         * Override
         */
        readBamFeatures: function(ba, blockStart, sink, callback) {
            var that = this;
            var featureCount = 0;

            var maxFeaturesWithoutYielding = 300;

            while ( true ) {
                if( blockStart >= ba.length ) {
                    // if we're done, call the callback and return
                    callback( sink );
                    return;
                }
                else if( featureCount <= maxFeaturesWithoutYielding ) {
                    // if we've read no more than 200 features this cycle, read another one
                    var blockSize = readInt(ba, blockStart);
                    var blockEnd = blockStart + 4 + blockSize - 1;

                    // only try to read the feature if we have all the bytes for it
                    if( blockEnd < ba.length ) {
                        var feature = new WebApolloBAMFeature({
                            store: this.store,
                            file: this,
                            bytes: { byteArray: ba, start: blockStart, end: blockEnd }
                        });
                        sink.push(feature);
                        featureCount++;
                    }

                    blockStart = blockEnd+1;
                }
                else {
                    // if we're not done but we've read a good chunk of
                    // features, put the rest of our work into a timeout to continue
                    // later, avoiding blocking any UI stuff that's going on
                    window.setTimeout( function() {
                        that.readBamFeatures( ba, blockStart, sink, callback );
                    }, 1);
                    return;
                }
            }
        }
    });

    return WebApolloBAMFile;

});