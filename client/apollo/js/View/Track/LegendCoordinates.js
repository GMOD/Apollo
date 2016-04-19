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
        this.svgCoords.fCoord = [];
        this.svgCoords.tCoord = [];
        this.svgCoords.bCoord = [];


        this.svgHeight = 100;
        this.svgScale = 1;

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

    showRange: function(first, last, startBase, bpPerBlock, scale, containerStart, containerEnd) {
        console.log("SVGLayerCoords::showRange");

        this.inherited( arguments );

        // adjust svg size
        var left = first * this.svgParent.widthPct;
        var width = (last - first + 1) * this.svgParent.widthPct;

        // coords group
        this.svgCoords.setAttribute('style', 'left:'+left+'%;width:'+width+'%;height:100%;position:absolute;z-index:15');
        this.coordGroup.setAttribute('style', 'width:100%;height:100%;position:absolute;');

        // erase test coordinates
        for (var bpCoord in this.svgCoords.fCoord) {
            this.svgCoords.fCoord[bpCoord].setAttribute("display","none");
            this.svgCoords.tCoord[bpCoord].setAttribute("display","none");
            this.svgCoords.bCoord[bpCoord].setAttribute("display","none");
        }

        // draw test coordinates
        for(i=first;i < last;i++) {
            var bpCoord = this.svgParent.blocks[i].startBase;
            var coordinateLabel = this.calculateBpForSequence(bpCoord+1);
            var x = this.bp2Native(bpCoord);
            var svgCoord;
            if (bpCoord in this.svgCoords.fCoord) {
                svgCoord = this.svgCoords.fCoord[bpCoord];
            }
            else {
                svgCoord = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                this.svgCoords.fCoord[bpCoord] = svgCoord;
            }
            var xlength = 5; // for 0 case only
            //var formattedLabel = numberWithCommas(bpCoord + 1);
            var formattedLabel = numberWithCommas(coordinateLabel);
            var offsetMultiplier = 5;
            if (bpCoord +1 != 0) {
                xlength = -(formattedLabel.length - 1) * offsetMultiplier;
            }
            svgCoord.setAttribute('x', x + xlength);
            svgCoord.setAttribute('y', 30);
            svgCoord.setAttribute('fill', 'blue');
            //svgCoord.setAttribute('transform','rotate(90 '+x+' 20)');
            svgCoord.setAttribute('display', 'block');
            svgCoord.innerHTML = formattedLabel;
            this.coordGroup.appendChild(svgCoord);
        }

        for(var i=first;i < last;i++) {
            bpCoord = this.svgParent.blocks[i].startBase;
            x = this.bp2Native(bpCoord);
            var topTick;
            if (bpCoord in this.svgCoords.tCoord) {
                topTick= this.svgCoords.tCoord[bpCoord];
            }
            else {
                topTick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                this.svgCoords.tCoord[bpCoord] = topTick;
            }

            // draw stems
            //var topTick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
            topTick.setAttribute('x1', x);
            //topTick.setAttribute('y1',len);
            topTick.setAttribute('y1', 0);
            topTick.setAttribute('x2', x);
            topTick.setAttribute('y2', 10);
            topTick.setAttribute('stroke', 'rgba(0,0,0,.5)');
            topTick.setAttribute('stroke-width', 2);
            topTick.setAttribute('stroke-linecap', 'round');
            topTick.setAttribute('display', 'block');
            this.coordGroup.appendChild(topTick);
        }

        for(var i=first;i < last;i++) {
            var bpCoord = this.svgParent.blocks[i].startBase;
            var x = this.bp2Native(bpCoord);
            var bottomTick;
            if (bpCoord in this.svgCoords.bCoord) {
                bottomTick = this.svgCoords.bCoord[bpCoord];
            }
            else {
                bottomTick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
                this.svgCoords.bCoord[bpCoord] = bottomTick;
            }

            //var bottomTick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
            bottomTick.setAttribute('x1', x);
            //bottomTick.setAttribute('y1',len);
            bottomTick.setAttribute('y1', 40);
            bottomTick.setAttribute('x2', x);
            bottomTick.setAttribute('y2', 50);
            bottomTick.setAttribute('stroke', 'rgba(0,0,0,.5)');
            bottomTick.setAttribute('stroke-width', 2);
            bottomTick.setAttribute('stroke-linecap', 'round');
            bottomTick.setAttribute('display', 'block');
            this.coordGroup.appendChild(bottomTick);
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
