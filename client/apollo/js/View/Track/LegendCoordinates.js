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
        this.svgCoords.fCoord = new Array();


        this.svgHeight = 100;
        this.svgScale = 1;

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

        var maxLen = this.svgHeight;
        var len = 0;

        // erase test coordinates
        for (var bpCoord in this.svgCoords.fCoord) {
            this.svgCoords.fCoord[bpCoord].setAttribute("display","none");
        }

        // draw test coordinates
        for(var i=first;i < last;i++) {
            var bpCoord = this.svgParent.blocks[i].startBase;
            var x = this.bp2Native(bpCoord);
            var svgCoord;
            if (bpCoord in this.svgCoords.fCoord ) {
                svgCoord = this.svgCoords.fCoord[bpCoord];
            }
            else {
                svgCoord = document.createElementNS('http://www.w3.org/2000/svg','text');
                this.svgCoords.fCoord[bpCoord] = svgCoord;
            }
            var xlength = 5 ; // for 0 case only
            var formattedLabel = numberWithCommas(bpCoord+1);
            var offsetMultiplier = 5 ;
            if(x!=0){
                xlength = - (formattedLabel.length-1) * offsetMultiplier ;
            }
            svgCoord.setAttribute('x',x+xlength);
            svgCoord.setAttribute('y',30);
            svgCoord.setAttribute('fill','blue');
            //svgCoord.setAttribute('transform','rotate(90 '+x+' 20)');
            svgCoord.setAttribute('display','block');
            svgCoord.innerHTML = formattedLabel;
            this.coordGroup.appendChild(svgCoord);

            // draw stems
            var topTick = document.createElementNS('http://www.w3.org/2000/svg','line');
            topTick.setAttribute('x1',x);
            //topTick.setAttribute('y1',len);
            topTick.setAttribute('y1',0);
            topTick.setAttribute('x2',x);
            topTick.setAttribute('y2',10);
            topTick.setAttribute('stroke','rgba(0,0,0,.5)');
            topTick.setAttribute('stroke-width',2);
            topTick.setAttribute('stroke-linecap','round');
            this.coordGroup.appendChild(topTick);

            var bottomTick = document.createElementNS('http://www.w3.org/2000/svg','line');
            bottomTick.setAttribute('x1',x);
            //bottomTick.setAttribute('y1',len);
            bottomTick.setAttribute('y1',40);
            bottomTick.setAttribute('x2',x);
            bottomTick.setAttribute('y2',50);
            bottomTick.setAttribute('stroke','rgba(0,0,0,.5)');
            bottomTick.setAttribute('stroke-width',2);
            bottomTick.setAttribute('stroke-linecap','round');
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
