define( [
        'dojo/_base/declare',
        'dojo/_base/array',
        'WebApollo/ProjectionUtils',
        'JBrowse/Store/SeqFeature/BAM/LazyFeature'
    ],
    function(
        declare,
        array,
        ProjectionUtils,
        BAMFeature
    ) {

    return declare(BAMFeature, {

        /**
         * Override
         */
        id: function() {
            if (this._get('_original_start')) {
                return this._get('name')+'/'+this._get('md')+'/'+this._get('cigar')+'/'+this._get('_original_start')+'/'+this._get('multi_segment_next_segment_reversed');
            }
            else {
                return this._get('name')+'/'+this._get('md')+'/'+this._get('cigar')+'/'+this._get('start')+'/'+this._get('multi_segment_next_segment_reversed');
            }
        },

        /*
         * Override
         */
        subfeatures: function() {
            var cigar = this._get('cigar');
            if (cigar) {
                var sequenceListObject = ProjectionUtils.parseSequenceList(this.file.store.refSeq.name);
                if (sequenceListObject[0].reverse) {
                    var cigar_array = cigar.match(/\d+\D/g);
                    cigar_array.reverse();
                    cigar = cigar_array.join('');
                }
                return this._cigarToSubfeats(cigar);
            }
        }

    });
});