define([ 'dojo/_base/declare',
        'dojo/_base/array'
    ],
    function(
        declare,
        array
    ) {

        function ProjectionUtils() {}

        ProjectionUtils.NOT_YET_SUPPORTED_MESSAGE = "Reverse complement view with local tracks not yet supported.";

        ProjectionUtils.isSequenceList = function (refSeqName){
            if(refSeqName.indexOf('{')<0){
                return false ;
            }
            return true ;
        };
        /**
         * Parse sequenceList string and return a JSON object
         */
        ProjectionUtils.parseSequenceList = function( refSeqName ) {
            var refSeqNameSplit = refSeqName.split(':');
            var sequenceListString = refSeqNameSplit.slice(0, refSeqNameSplit.length - 1).join(':');
            var sequenceListObject = JSON.parse(sequenceListString).sequenceList;
            return sequenceListObject;
        };

        /**
         * Project the given start and end
         */
        ProjectionUtils.projectCoordinates =  function( refSeqName, start, end ) {
            var projectedStart = parseInt(window.parent.projectValue(refSeqName, start).toString());
            var projectedEnd = parseInt(window.parent.projectValue(refSeqName, end).toString());
            if (projectedStart > projectedEnd) {
                start = projectedEnd;
                end = projectedStart;
            }
            else {
                start = projectedStart;
                end = projectedEnd;
            }
            return [start, end];
        };

        /**
         * Projects a single coordinate
         * @param refSeqName
         * @param start
         * @param end
         * @returns {*[]}
         */
        ProjectionUtils.projectCoordinate =  function( refSeqName, input) {
            var projectedInput = parseInt(window.parent.projectValue(refSeqName, input ).toString());
            return projectedInput ;
        };

        ProjectionUtils.projectStrand = function(refSeqName,input){
            if(!input) return input ;

            var sequenceListObject = this.parseSequenceList(refSeqName);
            if(sequenceListObject[0].reverse){
                return input < 0 ? 1 : -1 ;
            }
            else{
                return input ;
            }
        };

        ProjectionUtils.flipStrand = function(input){
            if(input==='+') return '-';
            if(input==='-') return '+';
            return input ;
        };

        ProjectionUtils.unProjectGFF3 = function(refSeqName,line){
            var returnArray ;
            returnArray = line.split("\t");
            var coords = this.unProjectCoordinates(refSeqName,returnArray[3],returnArray[4]);
            var sequenceListObject = this.parseSequenceList(refSeqName);
            if(sequenceListObject[0].reverse){
                returnArray[6] = this.flipStrand(returnArray[6]);
            }
            returnArray[3] = coords[0];
            returnArray[4] = coords[1];
            return returnArray.join("\t")
        };

        /**
         * Unproject the given start and end
         */
        ProjectionUtils.unProjectCoordinates = function( refSeqName, start, end ) {
            var sequenceListObject = this.parseSequenceList(refSeqName);
            var projectionLength = parseInt(window.parent.getProjectionLength(refSeqName).toString());

            if (start > 0) {
                if (end < 0) {
                    // request is to fetch something near the end of the projection
                    // and the requested end is outside the projection boundary.
                    // Thus, adjust end to max length of current projection
                    end = sequenceListObject[0].end;
                }
                else {
                    // request is to fetch something near the end of the projection
                    // and the requested end is outside the projection boundary.
                    // Thus, adjust end to max length of current projection
                    if (start < projectionLength && end > projectionLength) end = projectionLength;
                }
            }
            else if (start < 0) {
                if (end < 0) {
                    // do nothing
                }
                else {
                    // request is to fetch something at the start of the projection
                    // and the requested start is outside the projection boundary.
                    // Thus, adjust start to 0
                    start = 0;
                }
            }

            var unprojectedStart = parseInt(window.parent.unProjectValue(refSeqName, start).toString());
            var unprojectedEnd = parseInt(window.parent.unProjectValue(refSeqName, end).toString());
            if (unprojectedStart > unprojectedEnd) {
                // in a reverse projection, unprojected start will always be greater than unprojected end
                start = unprojectedEnd;
                end = unprojectedStart;
            }
            else {
                start = unprojectedStart;
                end = unprojectedEnd;
            }

            return [start, end];
        };

        /**
         * Project a given JSON feature
         */
        ProjectionUtils.unprojectJSONFeature = function( feature ) {
            if(!feature.isProjected) return feature ;
            feature.data.start = feature.data._original_start ;
            feature.data.end = feature.data._original_end ;
            feature.data.strand = feature.data._original_strand;
            delete feature.data._original_start;
            delete feature.data._original_end ;
            delete feature.data._original_strand ;
            feature.isProjected = false;
            if (feature.data.subfeatures) {
                for (var i = 0; i < feature.data.subfeatures.length; i++) {
                    this.unprojectJSONFeature(feature.data.subfeatures[i]);
                }
            }
            return feature;
        };

        /**
         * Project a given JSON feature
         */
        ProjectionUtils.projectJSONFeature = function( feature, refSeqName ) {
            if(feature.isProjected) {
                consoel.log('already projected');
                return feature ;
            }

            var start = feature.get("start");
            var end = feature.get("end");
            var strand = feature.get("strand");
            var projectedArray = this.projectCoordinates(refSeqName, start, end);
            if (!feature.data) {
                feature.data = {};
            }
            feature.data._original_start = start;
            feature.data._original_end = end;
            feature.data._original_strand = strand;


            feature.data.start = projectedArray[0];
            feature.data.end = projectedArray[1];
            // feature.data.strand = this.flipStrand(feature.data._original_strand);
            feature.data.strand = this.projectStrand(refSeqName,feature.data.strand);
            feature.isProjected = true;
            if (feature.data.subfeatures) {
                for (var i = 0; i < feature.data.subfeatures.length; i++) {
                    this.projectJSONFeature(feature.data.subfeatures[i], refSeqName);
                }
            }
            return feature;
        };

        return ProjectionUtils;
});