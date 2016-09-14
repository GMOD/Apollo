package org.bbop.apollo.sequence

/**
 * Created by Nathan Dunn on 2/19/15.
 */
enum Strand {

    POSITIVE(1,"+"),
    NEGATIVE(-1,"-"),
    NONE(0,".")

    Integer value
    String display

    public Strand(Integer value,String display) {
        this.value = value
        this.display = display
    }

    static Strand getStrandForValue(Integer i) {
        if(i!=null){
            for(strand in values()){
                if(strand.value==i) {
                    return strand
                }
            }
        }
        return NONE
    }

    public getValue() {
        return this.value
    }

    String getDisplay() {
        return display
    }
}