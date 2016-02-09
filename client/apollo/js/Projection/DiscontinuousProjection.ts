///<reference path="ProjectionInterface.ts"/>

class DiscontinuousProjection implements ProjectionInterface{

    sequence:ProjectionSequence;

    // TODO: does typesafe have these types of structures?
    //TreeMap<Integer, Coordinate> minMap = new TreeMap<>()
    //TreeMap<Integer, Coordinate> maxMap = new TreeMap<>()

    projectValue(input:number):number {
        return null;
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
        return new Coordinate(this.projectReverseValue(min), this.projectReverseValue(max), this.sequence);
    }
}