define(['dojo/_base/declare',
        'dojo/_base/lang',
        'dojo/_base/array',
        'JBrowse/Store/SeqFeature/BigWig'
    ],
    function (declare,
              lang,
              array,
              BigWig) {
        return declare([BigWig],
            {

                getApollo: function () {
                    return window.parent;
                },
                _getFeatures: function (query, featureCallback, endCallback, errorCallback) {

                    var chrName = query.ref;
                    var min = query.start;
                    var max = query.end;

                    var unprojectedMin = this.getApollo().unProjectValue(chrName,min);
                    var unprojectedMax = this.getApollo().unProjectValue(chrName,max);
                    var sequenceNames = JSON.parse(this.getApollo().getSequenceNames(chrName));
                    console.log(sequenceNames);

                    if(sequenceNames.length!=1){
                        alert('Failed to read sequence name properly');
                    }
                    chrName = sequenceNames[0];

                    var v = query.basesPerSpan ? this.getView(1 / query.basesPerSpan) :
                        query.scale ? this.getView(query.scale) :
                            this.getView(1);

                    if (!v) {
                        endCallback();
                        return;
                    }
                    // convert back down to lower-case
                    chrName = this.browser.regularizeReferenceName(chrName);

                    v.readWigData(chrName, unprojectedMin, unprojectedMax, dojo.hitch(this, function (features) {
                        array.forEach(features || [], featureCallback);
                        endCallback();
                    }), errorCallback);
                },
            });
    });
