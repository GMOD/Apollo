///<reference path="Coordinate.ts"/>
interface ProjectionInterface {

    projectValue(input: number): number;
    projectReverseValue(input: number): number;

    projectCoordinate(min: number,max: number): Coordinate;
    projectReverseCoordinate(min: number,max: number): Coordinate;

    projectSequences(inputSequence: string,min: number , max: number, offset: number): string;
    getLength(): number
    clear(): number
}
