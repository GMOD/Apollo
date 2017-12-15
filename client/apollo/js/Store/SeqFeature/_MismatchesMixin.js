define([
        'dojo/_base/declare',
        'dojo/_base/array',
        'JBrowse/Store/SeqFeature/_MismatchesMixin',
        'WebApollo/ProjectionUtils'
    ],
    function(
        declare,
        array,
        MismatchesMixin,
        ProjectionUtils
    ) {

        return declare( MismatchesMixin, {

            _mdToMismatches: function( feature, mdstring, cigarOps, cigarMismatches ) {
                var sequenceList = ProjectionUtils.parseSequenceList(this.browser.refSeq.name);
                var mismatchRecords = [];
                var curr = { start: 0, base: '', length: 0, type: 'mismatch' };

                // convert a position on the reference sequence to a position
                // on the template sequence, taking into account hard and soft
                // clipping of reads
                function getTemplateCoord( refCoord, cigarOps ) {
                    var templateOffset = 0;
                    var refOffset = 0;
                    for( var i = 0; i < cigarOps.length && refOffset <= refCoord ; i++ ) {
                        var op  = cigarOps[i][0];
                        var len = cigarOps[i][1];
                        if( op == 'S' || op == 'I' ) {
                            templateOffset += len;
                        }
                        else if( op == 'D' || op == 'P' ) {
                            refOffset += len;
                        }
                        else {
                            templateOffset += len;
                            refOffset += len;
                        }
                    }
                    return templateOffset - ( refOffset - refCoord );
                }

                function nextRecord() {
                    // correct the start of the current mismatch if it comes after a cigar skip
                    var skipOffset = 0;
                    array.forEach( cigarMismatches || [], function( mismatch ) {
                        if( mismatch.type == 'skip' && curr.start >= mismatch.start ) {
                            curr.start += mismatch.length;
                        }
                    });

                    // record it
                    mismatchRecords.push( curr );

                    // get a new mismatch record ready
                    curr = { start: curr.start + curr.length, length: 0, base: '', type: 'mismatch'};
                };

                var seq = feature.get('seq');
                if (feature.isProjected) {
                    if (sequenceList[0].reverse) {
                        var md_array = mdstring.match(/(\d+|\^[a-z]+|[a-z])/ig);
                        md_array.reverse();
                        mdstring = md_array.join('');

                        seq = seq.split('').reverse().join('');
                    }
                }

                // now actually parse the MD string
                array.forEach( mdstring.match(/(\d+|\^[a-z]+|[a-z])/ig), function( token ) {
                    if( token.match(/^\d/) ) { // matching bases
                        curr.start += parseInt( token );
                    }
                    else if( token.match(/^\^/) ) { // insertion in the template
                        curr.length = token.length-1;
                        curr.base   = '*';
                        curr.type   = 'deletion';
                        curr.seq    = token.substring(1);
                        nextRecord();
                    }
                    else if( token.match(/^[a-z]/i) ) { // mismatch
                        for( var i = 0; i<token.length; i++ ) {
                            curr.length = 1;
                            curr.base = seq ? seq.substr( cigarOps ? getTemplateCoord( curr.start, cigarOps)
                                    : curr.start,
                                1
                            )
                                : 'X';
                            curr.altbase = token;
                            nextRecord();
                        }
                    }
                });
                return mismatchRecords;
            },

            _parseCigar: function(cigar) {
                var sequenceListObject = ProjectionUtils.parseSequenceList(this.browser.refSeq.name);
                if (sequenceListObject[0].reverse) {
                    var cigar_array = cigar.match(/\d+\D/g);
                    cigar_array.reverse();
                    cigar = cigar_array.join('');
                }
                return array.map( cigar.toUpperCase().match(/\d+\D/g), function( op ) {
                    return [ op.match(/\D/)[0], parseInt( op ) ];
                });
            }

        });


});