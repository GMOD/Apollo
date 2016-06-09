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

    removeLabels: function(){
        for (var bpCoord in this.svgCoords.middleCoord) {
            this.svgCoords.middleCoord[bpCoord].setAttribute("display","none");
            this.svgCoords.topCoord[bpCoord].setAttribute("display","none");
            this.svgCoords.bottomCoord[bpCoord].setAttribute("display","none");
            this.svgCoords.shadedCoord[bpCoord].setAttribute("display","none");
        }
    },

    addSequenceLabel: function(bpCoord){

        var coordinateLabel = this.calculateBpForSequence(bpCoord+1);
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
        svgCoord.setAttribute('y', 30);
        svgCoord.setAttribute('fill', 'black');
        svgCoord.setAttribute('fill-opacity', '0.5');
        svgCoord.setAttribute('display', 'block');
        svgCoord.innerHTML = formattedLabel ;
        this.coordGroup.appendChild(svgCoord);
    },

    addTrackLabel: function(bpCoord){
        var label = this.getSequenceForBp(bpCoord);
        //if(label=='') continue ;
        var topTick;
        if (bpCoord in this.svgCoords.topCoord) {
            topTick= this.svgCoords.topCoord[bpCoord];
        }
        else {
            topTick = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            this.svgCoords.topCoord[bpCoord] = topTick;
        }

        var x = this.bp2Native(bpCoord);
        // draw stems
        var xlength = -((label.length - 1) * 3);
        topTick.setAttribute('x', x+xlength);
        topTick.setAttribute('y', 10);
        topTick.setAttribute('stroke-width', 0.5);
        topTick.setAttribute('stroke', 'black');
        topTick.setAttribute('display', 'block');
        topTick.innerHTML = label ;
        this.coordGroup.appendChild(topTick);
    },

    addBlockTick: function(bpCoord){
        var x = this.bp2Native(bpCoord);
        var bottomTick;
        if (bpCoord in this.svgCoords.bottomCoord) {
            bottomTick = this.svgCoords.bottomCoord[bpCoord];
        }
        else {
            bottomTick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
            this.svgCoords.bottomCoord[bpCoord] = bottomTick;
        }

        bottomTick.setAttribute('x1', x);
        bottomTick.setAttribute('y1', 40);
        bottomTick.setAttribute('x2', x);
        bottomTick.setAttribute('y2', 50);
        bottomTick.setAttribute('stroke', 'rgba(0,0,0,.5)');
        bottomTick.setAttribute('stroke-width', 2);
        bottomTick.setAttribute('stroke-linecap', 'round');
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
        tick.setAttribute('d', 'M'+x+' 30 L'+(x-50)+' 0 L'+(x+50)+' 0 Z');
        tick.setAttribute('fill', this.getColorForBp(bpCoord));
        tick.setAttribute('fill-opacity', 0.1);
        tick.setAttribute('display', 'block');
        this.coordGroup.appendChild(tick);
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
        for(var i=first;i < last;i++) {
            var bpCoord = this.svgParent.blocks[i].startBase;
            this.addSequenceLabel(bpCoord);
            this.addTrackLabel(bpCoord);
            this.addBlockTick(bpCoord);
            this.addSequenceTick(bpCoord);
        }
    },
    bp2Native: function(val) {
        return (val - this.svgParent.displayContext.startBase) * this.svgParent.displayContext.scale;
    },
    destroy: function() {

        domConstruct.destroy( this.svgCoords );
        delete this.svgCoords

        this.inherited( arguments );
    }
});
});
