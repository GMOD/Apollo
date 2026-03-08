package org.bbop.apollo;

public class Pair<T, U> {

    private T first;
    private U second;
    
    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
    
    public Pair(Pair<T, U> pair) {
        this.first = pair.first;
        this.second = pair.second;
    }
    
    public T getFirst() {
        return first;
    }
    
    public U getSecond() {
        return second;
    }
    
    public String toString() {
        return "[" + first.toString() + ", " + second.toString() + "]";
    }
}
