
module Projection{

    export class ProjectionSequence {

        id:string;
        name:string;
        organism:string;

        order:number;  // what order this should be processed as
        offset:number;
        originalOffset:number = 0 ; // original incoming coordinates . .  0 implies order = 0, >0 implies that order > 0
        features:Array<string>;
        unprojectedLength:number = 0;
        //List<String> features// a list of Features  // default is a single entry ALL . . if empty then all
        //Integer unprojectedLength = 0  // the length of the sequence before projection . . the projected length comes from the associated discontinuous projection

        constructor() {
        }

        test():string {
            return "test string";
        }

        static blink():void {
            console.log("blink");
        }
    }
}
