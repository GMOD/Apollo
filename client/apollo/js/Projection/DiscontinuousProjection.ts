///<reference path="AbstractProjection.ts"/>
///<reference path="node-0.12.d.ts" />
///<reference path="collections.ts" />


//import collections = require('collections');
import SortedMap = require("collections/sorted-map");

//var LruMap = require("collections/lru-map");

class DiscontinuousProjection extends AbstractProjection {

    minMap:SortedMap<number,Coordinate>;
    maxMap:SortedMap<number,Coordinate>;


    constructor() {
        //var t = new collections.BSTree();
        minMap = new SortedMap<number,Coordinate>();
        maxMap = new SortedMap<number,Coordinate>();
    }


    // TODO: does typesafe have these types of structures?
    //TreeMap<number, Coordinate> minMap = new TreeMap<>()
    //TreeMap<number, Coordinate> maxMap = new TreeMap<>()

    projectValue(input:number):number {
        if (!minMap && !maxMap) {
            return input;
        }

        if (input == null) {
            return this.UNMAPPED_VALUE;
        }

        floorMinKey = this.minMap.floorKey(input);
        ceilMinKey = this.minMap.ceilingKey(input);

        floorMaxKey = this.maxMap.floorKey(input);
        ceilMaxKey = this.maxMap.ceilingKey(input);

//        log.debug "input ${input} minKey ${floorMinKey}-${ceilMinKey}"
//        log.debug "input ${input} maxKey ${floorMaxKey}-${ceilMaxKey}"

        if (floorMinKey == null || ceilMaxKey == null) {
            return this.UNMAPPED_VALUE;
        }

        // if is a hit for min and no max hit, then it is the left-most
        if (floorMinKey == ceilMinKey) {
            if (floorMaxKey == null) {
                return 0
            } else {
//                return input - floorMaxKey
                return projectValue(floorMaxKey) + 1
            }
        }

        // this is the left-most still
        if (floorMinKey != ceilMinKey && floorMaxKey == null) {
            return input - floorMinKey
        }


        // if we are at the max border
        if (floorMaxKey == ceilMaxKey) {
            return input - floorMinKey + projectValue(floorMinKey)
        }

        // if we are inbetween a ceiling max and floor min, then we are in a viable block
        if (input > floorMinKey && input < ceilMaxKey && ceilMinKey >= ceilMaxKey) {
            return input - floorMinKey + projectValue(floorMinKey)
        }

        // if we are inbetween for the last large one on the RHS
        if (floorMaxKey != ceilMaxKey && ceilMinKey == null) {
            return input - floorMinKey + projectValue(floorMinKey)
        }

//        log.debug "${input} unable to find match, returning UNMAPPED"
        return UNMAPPED_VALUE
    }

    projectReverseValue(input:number):number {
        return null;
    }

    projectSequences(inputSequence:string, min:number, max:number, offset:number):string {
        return null;
    }

    getLength():number {
        return null;
    }

    clear():number {
        return null;
    }

    projectCoordinate(min:number, max:number):Coordinate {
        return new Coordinate(this.projectValue(min), this.projectValue(max), this.sequence);
    }

    projectReverseCoordinate(min:number, max:number):Coordinate {
        newMin = projectReverseValue(min);
        newMax = projectReverseValue(max);
        if (newMin < 0 && newMax < 0) return null;
    }

    getOriginalLength():number {
        return maxMap.values().last().max
    }

    getBufferedLength(buffer:number):number {
        number = number ? number : 1;
        return length + buffer * (size() - 1)
    }

    getLength():number {
        returnValue = 0;
        iter = minMap.values();
        //while(iter.hasNext()){
        //    Coordinate coordinate = iter.next().value();
        //    returnValue += coordinate.length;
        //}
        for (k of minMap.keys()) {
            coordinate = this.minMap.get(k)
            returnValue += coordinate.length;
            //statement
        }
        //for (var k in iter) {
        //    var value = minMap[key];
        //    // Use `key` and `value`
        //}
        //foreach (Coordinate coordinate : minMap.values() ) {
        //    returnValue += coordinate.length;
        //}
        return returnValue;
    }

    projectSequence(inputSequence:string, minCoordinate:number, maxCoordinate:number):string {
        projectSequence(inputSequence, minCoordinate, maxCoordinate, 0);
    }

    projectSequence(inputSequence:string, minCoordinate:number, maxCoordinate:number, offset:number):string {
        var returnSequence:string = "";
        //Iterator<Coordinate> minKeyIterator = this.minMap.values().iterator();
        var minKeyIterator:Iterator<number> = this.minMap.values().iterator();
        var minCoordinate:Coordinate = minCoordinate >= 0 ? minCoordinate : 0;
        var maxCoordinate:Coordinate = maxCoordinate >= 0 ? maxCoordinate : inputSequence.length();
        //console.log "minCoordinate = ${minCoordinate}"
        //console.log "maxCoordinate = ${maxCoordinate}"
        //console.log "offset = ${offset}"
        //console.log "# of min maps ${minMap.size()}"

        while (minKeyIterator.hasNext()) {
            var coordinate:Coordinate = minKeyIterator.next();
            console.log("coodinate coord ${coordinate.min}::${coordinate.max} vs ${inputSequence.length()}");
            var offsetMinCoordinate:number = coordinate.min + offset;
            var offsetMaxCoordinate:number = coordinate.max + offset;
            //console.log
            //"offset coord ${offsetMinCoordinate}::${offsetMaxCoordinate} vs ${inputSequence.length()}"
            //console.log
            //"min/max ${minCoordinate}::${maxCoordinate} vs ${inputSequence.length()}"
            // 6 cases
            // case 1, max < minCoordinate . . .ignore
            // case 5, min > maxCoordinate  . . .ignore
            if (offsetMaxCoordinate < minCoordinate || offsetMinCoordinate > maxCoordinate) {
                // do nothing
            }
// case 6, overlaps all the way, min < minCoordinate, max > maxCoordinate, add minCoorindate, maxCoordinate
            else if (offsetMinCoordinate <= minCoordinate && offsetMaxCoordinate >= maxCoordinate) {
                returnSequence += inputSequence.substring(minCoordinate, maxCoordinate + 1)
            }
// case 2, left-hand edge , min < minCoordinate , max > minCoordinate, add minCoordinate, max
            else if (offsetMinCoordinate <= minCoordinate && offsetMaxCoordinate >= minCoordinate) {
                returnSequence += inputSequence.substring(minCoordinate, offsetMaxCoordinate + 1)
            }
// case 3, inside min > minCoordinate , max < maxCoordinate . . . add as-is, min/ max
            else if (offsetMinCoordinate > minCoordinate && offsetMaxCoordinate <= maxCoordinate) {
                returnSequence += inputSequence.substring(offsetMinCoordinate, offsetMaxCoordinate + 1)
            }
// case 4, right-hand edge min < maxCoordinate , max > maxCoordinate . . . add min, maxCoordinate
            else if (offsetMinCoordinate <= maxCoordinate && offsetMaxCoordinate > maxCoordinate) {
                returnSequence += inputSequence.substring(offsetMinCoordinate, maxCoordinate + 1)
            } else {
                console.log("what is this error case? ");
            }

        }

        return returnSequence
    }


    toString():string {
        var returnString:string = "DiscontinuousProjection{";

        //this.minMap.values().each { it ->
        //    returnString += "[${it.min}::${it.max}]"
        //}

        returnString += '}';

        return returnString;
    }

    size():number {
        if (!this.minMap) {
            return 0
        }
        //assert minMap.size() == maxMap.size()
        return this.minMap.size()
    }

    clear():number {
        var returnValue:number = minMap.size();
//        assert returnValue == maxMap.size()
        minMap.clear();
        maxMap.clear();
        return returnValue;
    }
}