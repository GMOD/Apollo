package org.bbop.apollo

/**
 * Top level track feature
 */
class FeatureLazyResidues extends Feature{


    static constraints = {
        fmin nullable: false
        fmax nullable: false
    }

    Integer fmin;
    Integer fmax;
}
