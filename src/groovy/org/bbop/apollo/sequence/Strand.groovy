package org.bbop.apollo.sequence

/**
 * Created by ndunn on 2/19/15.
 */
enum Strand {

    POSITIVE(1)
    , NEGATIVE(-1)

    Integer value

    public Strand(Integer value) {
        this.value = value
    }

    public getValue() {
        return this.value
    }
}