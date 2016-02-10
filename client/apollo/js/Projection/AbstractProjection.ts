///<reference path="Coordinate.ts"/>
abstract class AbstractProjection implements ProjectionInterface{

    public static UNMAPPED_VALUE : number = -1;

    abstract projectValue(input:number):number ;
    abstract projectReverseValue(input:number):number ;
    abstract projectSequences(inputSequence:string, min:number, max:number, offset:number):string ;
    abstract getLength():number ;

    projectCoordinate(min:number, max:number):Coordinate {
        return new Coordinate(this.projectValue(min),this.projectValue(max));
    }

    projectReverseCoordinate(min:number, max:number):Coordinate {
        return new Coordinate(this.projectReverseValue(min),this.projectReverseValue(max))
    }

    clear():number {
        return 0;
    }
}
