/* 
 * SVG Layer - display coordinates across the track
 */

define( [
            'dojo/_base/declare',
            'dojo/_base/array',
            'dojo/_base/lang',
            'dojo/_base/event',
            'dojo/dom-construct',
            'JBrowse/View/Track/SVG/SVGLayerBase'
        ],
        function(
            declare,
            array,
            lang,
            domEvent,
            domConstruct,
            SVGLayerBase
        ) {
            function numberWithCommas(x) {
                return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
            }

return declare(
    [ SVGLayerBase ], {

    setViewInfo: function( genomeView, heightUpdate, numBlocks, trackDiv, widthPct, widthPx, scale ) {
        console.log("SVGLayerCoords::setViewInfo");

        this.inherited( arguments );
        //console.log('setting view info in LegendCoordinates: '+arguments);
        //console.log(arguments);

        // make svg canvas coord group
        this.svgCoords = document.createElementNS('http://www.w3.org/2000/svg','svg');
        this.svgCoords.setAttribute('class', 'svg-coords');
        this.svgCoords.setAttribute('style', 'width:100%;height:100%;cursor:default;position:absolute;z-index:15');
        this.svgCoords.setAttribute('version', '1.1');
        this.svgCoords.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
        this.svgCoords.setAttribute('xmlns:xlink', 'http://www.w3.org/1999/xlink');
        domConstruct.place(this.svgCoords,trackDiv);

        // container for coord elements (this is just to test the coordinate space)
        this.coordGroup = document.createElementNS('http://www.w3.org/2000/svg','g');
        this.svgCoords.appendChild(this.coordGroup);
        this.svgCoords.middleCoord = [];
        this.svgCoords.topCoord = [];
        this.svgCoords.bottomCoord = [];
        this.svgCoords.shadedCoord = [];


        this.svgHeight = 100;
        this.svgScale = 1;

    },

    getSequenceForBp: function(bp){
        var seqList =  this.svgParent.refSeq.sequenceList;
        for(var seq in seqList){
            var seqValue = seqList[seq];
            var offset = seqValue.offset ? seqValue.offset : 0 ;
            if(bp >= offset && bp <= offset + seqValue.length){
                return seqValue.name ;
            }
        }
        return '';
    },

    getColorForBp: function(bp){
        var seqList =  this.svgParent.refSeq.sequenceList;
        for(var seq in seqList){
            var seqValue = seqList[seq];
            var offset = seqValue.offset ? seqValue.offset : 0 ;
            if(bp >= offset && bp <= offset + seqValue.length){
                return seqValue.color;
            }
        }
        return '';
    },

    calculateBpForSequence: function(bp){
        var seqList =  this.svgParent.refSeq.sequenceList;
        for(var seq in seqList){
            var seqValue = seqList[seq];
            var offset = seqValue.offset ? seqValue.offset : 0 ;
            if(bp >= offset && bp <= offset + seqValue.length){
                return bp - offset + seqValue.start  ;
            }
        }
        return bp ;
    },

    // TODO: this is being called multiple times
    removeLabels: function(){
        for (var bpCoord in this.svgCoords.middleCoord) {
            this.svgCoords.middleCoord[bpCoord].setAttribute("display","none");
            if(this.svgCoords.topCoord[bpCoord]){
                this.svgCoords.topCoord[bpCoord].setAttribute("display","none");
            }
            //this.svgCoords.bottomCoord[bpCoord].setAttribute("display","none");
            this.svgCoords.shadedCoord[bpCoord].setAttribute("display","none");
        }
        console.log('removed labels');
    },

    addSequenceLabel: function(bpCoord){
        var coordinateLabel = this.calculateBpForSequence(bpCoord+1);
        if(!this.showSequenceLabel(coordinateLabel)){
            return ;
        }
        var x = this.bp2Native(bpCoord);
        var svgCoord;
        if (bpCoord in this.svgCoords.middleCoord) {
            svgCoord = this.svgCoords.middleCoord[bpCoord];
        }
        else {
            svgCoord = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            this.svgCoords.middleCoord[bpCoord] = svgCoord;
        }
        var xlength = 5; // for 0 case only
        var formattedLabel = numberWithCommas(coordinateLabel);
        var offsetMultiplier = 4;
        if (bpCoord +1 != 0) {
            xlength = -(formattedLabel.length - 1) * offsetMultiplier;
        }
        svgCoord.setAttribute('x', x + xlength);
        svgCoord.setAttribute('y', 13);
        svgCoord.setAttribute('fill', 'white');
        svgCoord.setAttribute('weight', 'bolder');
        //svgCoord.setAttribute('fill-opacity', '0.5');
        svgCoord.setAttribute('display', 'block');
        svgCoord.innerHTML = formattedLabel ;
        this.coordGroup.appendChild(svgCoord);
    },

    showSequenceLabel: function(label){
        if(label=='0'){
            return false;
        }
        return true ;
    },

    showTrackLabel: function(label){
        if(label==''){
            return false;
        }
        return true ;
    },

    addTrackLabel: function(bpCoord){
        x = this.bp2Native(bpCoord);
        var label = this.getSequenceForBp(bpCoord);
        if(!this.showTrackLabel(label)){
            return ;
        }
        var topTick;
        if (bpCoord in this.svgCoords.topCoord) {
            topTick= this.svgCoords.topCoord[bpCoord];
        }
        else {
            topTick = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            this.svgCoords.topCoord[bpCoord] = topTick;
        }

        // draw stems
        var xlength = -((label.length - 1) * 3);
        topTick.setAttribute('x', x+xlength+100);
        topTick.setAttribute('y', 12);
        topTick.setAttribute('stroke-width', 0.5);
        //topTick.setAttribute('stroke', 'white');
        var color = this.getColorForBp(bpCoord);
        topTick.setAttribute('stroke', color);
        topTick.setAttribute('fill', color);
        topTick.setAttribute('display', 'block');
        topTick.innerHTML = label ;
        this.coordGroup.appendChild(topTick);
    },

    addBlockTick: function(startCoord,endCoord){
        var start = this.bp2Native(startCoord);
        var end = this.bp2Native(endCoord);
        var bottomTick;
        if (startCoord in this.svgCoords.bottomCoord) {
            bottomTick = this.svgCoords.bottomCoord[startCoord];
        }
        else {
            bottomTick = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            this.svgCoords.bottomCoord[startCoord] = bottomTick;
        }
        var color = this.getColorForBp(startCoord);
        if(!color){
            color = 'green';
        }

        bottomTick.setAttribute('x', start);
        bottomTick.setAttribute('y', 0);
        bottomTick.setAttribute('height', 20);
        bottomTick.setAttribute('width', end-start);
        bottomTick.setAttribute('fill', color);
        bottomTick.setAttribute('display', 'block');
        this.coordGroup.appendChild(bottomTick);
    },

    addSequenceTick: function (bpCoord) {
        var x = this.bp2Native(bpCoord);
        var tick;
        if (bpCoord in this.svgCoords.shadedCoord) {
            tick = this.svgCoords.shadedCoord[bpCoord];
        }
        else {
            tick = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            this.svgCoords.shadedCoord[bpCoord] = tick;
        }
        //tick.setAttribute('d', 'M'+x+' 30 L'+(x-50)+' 0 L'+(x+50)+' 0 Z');
        tick.setAttribute('d', 'M'+x+' 30 L'+(x-50)+' 20 L'+(x-50)+' 0 L'+(x+50)+ ' 0 L'+(x+50)+' 20 Z');
        tick.setAttribute('fill', this.getColorForBp(bpCoord));
        //tick.setAttribute('fill-opacity', 0.1);
        tick.setAttribute('display', 'block');
        this.coordGroup.appendChild(tick);
    },

    /**
     * If the start of the block is greater than or equal to the start of a sequenceList,
     * but less than its own length or the length of the block, then return the start
     * else report -1
     * @param block
     * @returns {*}
     */
    getStartBorder: function(block){
        var start = block.startBase ;
        var end = block.endBase ;
        var seqList =  this.svgParent.refSeq.sequenceList;
        for(var seq in seqList){
            var seqValue = seqList[seq];
            var offset = seqValue.offset ? seqValue.offset : 0 ;
            //if(start < offset && )
            //if(bp >= offset && bp <= offset + seqValue.length){
            //    return bp - offset + seqValue.start  ;
            //}
        }
        return -1 ;
    },

    getEndBorder: function(block){

    },

    showRange: function(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {
        console.log("SVGLayerCoords::showRange");

        this.inherited( arguments );

        // adjust svg size
        var left = first * this.svgParent.widthPct;
        var width = (last - first + 1) * this.svgParent.widthPct;

        // coords group
        this.svgCoords.setAttribute('style', 'left:'+left+'%;width:'+width+'%;height:100%;position:absolute;z-index:15');
        this.coordGroup.setAttribute('style', 'width:100%;height:100%;position:absolute;');

        // TODO: there is a better way to erase
        // erase test coordinates
        this.removeLabels();

        // TODO: refactor for a single loop
        // draw test coordinates
        for(i=first;i < last;i++) {
            var startCoord = this.svgParent.blocks[i].startBase;
            var endCoord = this.svgParent.blocks[i].endBase;
            var startBorder = this.getStartBorder(this.svgParent.blocks[i]);
            var endBorder = this.getEndBorder(this.svgParent.blocks[i]);

            //this.addBlockTick(bpCoord,endCoord);
            if(startCoord>0){
                this.addSequenceTick(startCoord);
                this.addSequenceLabel(startCoord);
                this.addTrackLabel(startCoord);
            }
        }
    },
    bp2Native: function(val) {
        return (val - this.svgParent.displayContext.startBase) * this.svgParent.displayContext.scale;
    },
    destroy: function() {

        domConstruct.destroy( this.svgCoords );
        delete this.svgCoords;

        this.inherited( arguments );
    }
});
});
