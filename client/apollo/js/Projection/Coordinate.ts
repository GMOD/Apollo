///<reference path="ProjectionSequence.ts"/>



class Coordinate{
    min: number;
    max: number;
    sequence: ProjectionSequence;


    constructor(min:number, max:number, sequence:ProjectionSequence) {
        this.min = min;
        this.max = max;
        this.sequence = sequence;
    }


    static spitOutSomething():void{
        console.log("asdfasdfasdfa sdfasdlfkj ");
        //var returnString = 'whele that is workingish' ;
        //return returnString ;
    }
}