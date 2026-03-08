package org.bbop.apollo.sequence

/**
 * Created by nathandunn on 5/17/17.
 */
class SequenceLocationDTO {
    String sequenceName
    Integer startBp
    Integer endBp
    String clientToken

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        SequenceLocationDTO that = (SequenceLocationDTO) o

        if (clientToken != that.clientToken) return false

        return true
    }

    int hashCode() {
        int result = clientToken.hashCode()
        return result
    }
}
