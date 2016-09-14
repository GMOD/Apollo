package org.bbop.apollo.projection

/**
 * Created by nathandunn on 12/3/15.
 */
enum NclistColumnEnum {

        START,
        END,
        STRAND,
        SCORE,
        TYPE,
        SUBFEATURES,
        SUBLIST,
        SEQ_ID,
        ID,
        SOURCE,
        CHUNK,
        PHASE,


        private String value

        NclistColumnEnum(String value){
                this.value = value
        }

        NclistColumnEnum(){
                this.value = name().toLowerCase().capitalize()
        }

        String getValue() {
                return value
        }

}